package ke.securepay.core.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.result.IssuedKsIdentityResult;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@SecurePayIntegrationTest
class KsIdentityIssuanceConcurrencyIntegrationTest {

    @Autowired
    private KsIdentityIssuanceService issuanceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void concurrentUniqueIssuanceRequestsReceiveDistinctNumbers() throws Exception {
        String batch = UUID.randomUUID().toString();
        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<IssuedKsIdentityResult>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            int index = i;
            tasks.add(() -> {
                start.await(30, TimeUnit.SECONDS);
                var actor = ActorContextFactory.test("securepay-core");
                return issuanceService.issue(new IssueKsIdentityCommand(
                        "concurrent-" + batch + "-" + index, IdentityType.TEST, "C" + index, actor));
            });
        }

        List<Future<IssuedKsIdentityResult>> futures = pool.invokeAll(tasks);
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        List<IssuedKsIdentityResult> results = new ArrayList<>();
        for (Future<IssuedKsIdentityResult> future : futures) {
            results.add(future.get());
        }

        assertThat(results.stream().map(IssuedKsIdentityResult::identityId).collect(Collectors.toSet()))
                .hasSize(threads);
        assertThat(results.stream()
                        .map(r -> r.canonicalKsNumber().canonicalValue())
                        .collect(Collectors.toSet()))
                .hasSize(threads);
        assertThat(results.stream().map(IssuedKsIdentityResult::sequenceNumber).collect(Collectors.toSet()))
                .hasSize(threads);

        List<Long> sortedSequences = results.stream()
                .map(IssuedKsIdentityResult::sequenceNumber)
                .sorted()
                .toList();
        for (int i = 1; i < sortedSequences.size(); i++) {
            assertThat(sortedSequences.get(i)).isGreaterThan(sortedSequences.get(i - 1));
        }
    }

    @Test
    void concurrentDuplicateRequestsCreateOneIdentity() throws Exception {
        String batch = UUID.randomUUID().toString();
        String key = "concurrent-dup-" + batch;
        AtomicInteger executions = new AtomicInteger();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await(30, TimeUnit.SECONDS);
                    var actor = ActorContextFactory.test("securepay-core");
                    issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Dup", actor));
                    executions.incrementAndGet();
                } catch (Exception ignored) {
                    // concurrent idempotency races are expected
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        Integer identityCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity.ks_identities WHERE issuance_request_key = ?",
                Integer.class,
                key);
        assertThat(identityCount).isEqualTo(1);

        Integer auditCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM audit.audit_events
                WHERE event_type = ? AND metadata ->> 'issuance_request_key' = ?
                """,
                Integer.class,
                IdentityEventTypes.KS_NUMBER_ISSUED,
                key);
        assertThat(auditCount).isEqualTo(1);

        Integer outboxCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM events.outbox_events
                WHERE event_type = ? AND aggregate_id = (
                    SELECT id::text FROM identity.ks_identities WHERE issuance_request_key = ? LIMIT 1
                )
                """,
                Integer.class,
                IdentityEventTypes.KS_NUMBER_ISSUED,
                key);
        assertThat(outboxCount).isEqualTo(1);
    }
}
