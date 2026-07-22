package ke.securepay.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.persistence.exception.IdempotencyConflictException;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import ke.securepay.platform.persistence.idempotency.IdempotencyRepository;
import ke.securepay.platform.persistence.idempotency.IdempotencyService;
import ke.securepay.platform.persistence.idempotency.IdempotencyStatus;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@SecurePayIntegrationTest
class IdempotencyIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void firstAcquisitionCreatesInProgressThenCompletes() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String key = "idem_key_in_progress_001";
        String body = "{\"action\":\"technical-test\"}";

        var inProgress = idempotencyService.acquireTechnicalInProgress(actor, key, body, "application/json");
        assertThat(inProgress.processingStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);

        var completed = idempotencyService.completeTechnical(inProgress.id(), inProgress.version(), Map.of("result", "ok"));
        assertThat(completed.processingStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(completed.responseBody()).doesNotContainKey("password");
    }

    @Test
    void sameKeyAndHashReplaysSafelyWithoutReExecuting() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String key = "idem_key_first_001";
        String body = "{\"action\":\"technical-test\"}";

        var first = idempotencyService.executeTechnical(
                actor, key, body, "application/json", () -> Map.of("result", "ok"));
        assertThat(first.replayed()).isFalse();
        assertThat(first.record().processingStatus()).isEqualTo(IdempotencyStatus.COMPLETED);

        var replay = idempotencyService.executeTechnical(
                actor, key, body, "application/json", () -> Map.of("result", "should-not-run"));
        assertThat(replay.replayed()).isTrue();
    }

    @Test
    void sameKeyWithDifferentRequestHashIsRejected() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String key = "idem_key_conflict_001";
        idempotencyService.executeTechnical(actor, key, "{\"a\":1}", "application/json", () -> Map.of("ok", true));

        assertThatThrownBy(() -> idempotencyService.executeTechnical(
                        actor, key, "{\"a\":2}", "application/json", () -> Map.of("ok", true)))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void concurrentDuplicateAttemptsDoNotExecuteTwice() throws Exception {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String key = "idem_key_concurrent_001";
        String body = "{\"action\":\"concurrent\"}";
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Runnable task = () -> {
            try {
                start.await();
                idempotencyService.executeTechnical(
                        actor, key, body, "application/json", () -> {
                            executions.incrementAndGet();
                            return Map.of("count", executions.get());
                        });
            } catch (Exception ignored) {
                // race loser may observe in-progress conflict
            }
        };

        pool.submit(task);
        pool.submit(task);
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(executions.get()).isLessThanOrEqualTo(1);
    }

    @Test
    void optimisticLockConflictFailsWithoutAutomaticRetry() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        var inProgress = idempotencyService.acquireTechnicalInProgress(
                actor, "idem_key_optimistic_001", "{\"x\":1}", "application/json");

        assertThatThrownBy(() -> idempotencyService.completeTechnical(inProgress.id(), inProgress.version() + 99, Map.of("ok", true)))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void expiredRecordIsRejected() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String key = "idem_key_expired_001";
        var inProgress = idempotencyService.acquireTechnicalInProgress(actor, key, "{\"x\":1}", "application/json");

        jdbcTemplate.update(
                "UPDATE idempotency.idempotency_records SET expires_at = (NOW() AT TIME ZONE 'UTC') - INTERVAL '1 hour' WHERE id = ?",
                inProgress.id());

        assertThatThrownBy(() -> idempotencyService.executeTechnical(
                        actor, key, "{\"x\":1}", "application/json", () -> Map.of("ok", true)))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void staleInProgressLockRequiresRetry() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String key = "idem_key_stale_001";
        var inProgress = idempotencyService.acquireTechnicalInProgress(actor, key, "{\"x\":1}", "application/json");

        jdbcTemplate.update(
                "UPDATE idempotency.idempotency_records SET locked_until = (NOW() AT TIME ZONE 'UTC') - INTERVAL '1 minute' WHERE id = ?",
                inProgress.id());

        assertThatThrownBy(() -> idempotencyService.executeTechnical(
                        actor, key, "{\"x\":1}", "application/json", () -> Map.of("ok", true)))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("retry");
    }
}
