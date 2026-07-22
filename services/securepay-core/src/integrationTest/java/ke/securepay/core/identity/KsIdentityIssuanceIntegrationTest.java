package ke.securepay.core.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.exception.IssuanceOwnershipConflictException;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.persistence.idempotency.IdempotencyService;
import ke.securepay.platform.testing.contracts.EventEnvelopeSchemaSupport;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SecurePayIntegrationTest
class KsIdentityIssuanceIntegrationTest {

    private static JsonSchema eventSchema;

    @Autowired
    private KsIdentityIssuanceService issuanceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
        eventSchema = EventEnvelopeSchemaSupport.loadSchema();
    }

    @Test
    void sequentialIssuanceProducesDistinctCanonicalNumbers() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");

        var first = issuanceService.issue(new IssueKsIdentityCommand(
                "issue-seq-1-" + suffix, IdentityType.TEST, "First", actor));
        var second = issuanceService.issue(new IssueKsIdentityCommand(
                "issue-seq-2-" + suffix, IdentityType.TEST, "Second", actor));

        assertThat(first.identityId()).isNotEqualTo(second.identityId());
        assertThat(first.canonicalKsNumber().canonicalValue())
                .isNotEqualTo(second.canonicalKsNumber().canonicalValue());
        assertThat(first.sequenceNumber()).isLessThan(second.sequenceNumber());
    }

    @Test
    void issuanceCreatesAuditAndOutboxRecords() throws Exception {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.withCorrelation(
                ActorContextFactory.test("securepay-core"), "req_issue_" + suffix, "corr_issue_" + suffix);

        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "issue-audit-" + suffix, IdentityType.TEST, "Audit Test", actor));

        Integer auditCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM audit.audit_events
                WHERE event_type = ? AND resource_id = ?
                """,
                Integer.class,
                IdentityEventTypes.KS_NUMBER_ISSUED,
                issued.identityId().toString());
        assertThat(auditCount).isEqualTo(1);

        String payloadJson = jdbcTemplate.queryForObject(
                """
                SELECT payload::text FROM events.outbox_events
                WHERE aggregate_id = ? AND event_type = ?
                """,
                String.class,
                issued.identityId().toString(),
                IdentityEventTypes.KS_NUMBER_ISSUED);
        assertThat(payloadJson).isNotBlank();
        JsonNode payload = objectMapper.readTree(payloadJson);
        assertThat(eventSchema.validate(payload)).isEmpty();
        assertThat(payload.get("event_type").asText()).isEqualTo(IdentityEventTypes.KS_NUMBER_ISSUED);
        assertThat(payload.get("correlation_id").asText()).isEqualTo("corr_issue_" + suffix);
    }

    @Test
    void idempotentReplayReturnsSameIdentity() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        String key = "issue-idem-" + suffix;

        var first = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Replay", actor));
        var replay = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Replay", actor));

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.identityId()).isEqualTo(first.identityId());
        assertThat(replay.canonicalKsNumber()).isEqualTo(first.canonicalKsNumber());
    }

    @Test
    void sameKeyWithDifferentPayloadFails() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        String key = "issue-conflict-" + suffix;

        issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "A", actor));

        assertThatThrownBy(() -> issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "B", actor)))
                .isInstanceOf(IssuanceOwnershipConflictException.class);
    }

    @Test
    void replayDoesNotAdvanceSequenceOrCreateDuplicateEvents() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        String key = "issue-replay-seq-" + suffix;

        Long sequenceBefore = jdbcTemplate.queryForObject(
                "SELECT last_value FROM identity.ks_number_sequence", Long.class);

        var first = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Replay", actor));

        Long sequenceAfterFirst = jdbcTemplate.queryForObject(
                "SELECT last_value FROM identity.ks_number_sequence", Long.class);
        assertThat(sequenceAfterFirst).isGreaterThan(sequenceBefore);

        var replay = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Replay", actor));

        Long sequenceAfterReplay = jdbcTemplate.queryForObject(
                "SELECT last_value FROM identity.ks_number_sequence", Long.class);
        assertThat(sequenceAfterReplay).isEqualTo(sequenceAfterFirst);
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.identityId()).isEqualTo(first.identityId());

        Integer identityCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity.ks_identities WHERE issuance_request_key = ?",
                Integer.class,
                key);
        assertThat(identityCount).isEqualTo(1);

        Integer auditCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM audit.audit_events
                WHERE event_type = ? AND resource_id = ?
                """,
                Integer.class,
                IdentityEventTypes.KS_NUMBER_ISSUED,
                first.identityId().toString());
        assertThat(auditCount).isEqualTo(1);

        Integer outboxCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM events.outbox_events
                WHERE event_type = ? AND aggregate_id = ?
                """,
                Integer.class,
                IdentityEventTypes.KS_NUMBER_ISSUED,
                first.identityId().toString());
        assertThat(outboxCount).isEqualTo(1);
    }

    @Test
    void expiredIdempotencyRecordCannotAuthorizeSecondIdentityWhenOwnershipExists() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        String key = "issue-expired-idem-" + suffix;

        var first = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Stable", actor));

        jdbcTemplate.update(
                """
                UPDATE idempotency.idempotency_records
                SET created_at = (NOW() AT TIME ZONE 'UTC') - INTERVAL '2 hours',
                    updated_at = (NOW() AT TIME ZONE 'UTC') - INTERVAL '2 hours',
                    expires_at = (NOW() AT TIME ZONE 'UTC') - INTERVAL '1 hour',
                    locked_until = (NOW() AT TIME ZONE 'UTC') - INTERVAL '30 minutes'
                WHERE operation_code = ? AND idempotency_key = ?
                """,
                IdempotencyService.IDENTITY_ISSUE_OPERATION,
                key);

        var replay = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Stable", actor));

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.identityId()).isEqualTo(first.identityId());

        Integer identityCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity.ks_identities WHERE issuance_request_key = ?",
                Integer.class,
                key);
        assertThat(identityCount).isEqualTo(1);
    }

    @Test
    void deletedIdempotencyRecordCannotAuthorizeSecondIdentityWhenOwnershipExists() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        String key = "issue-deleted-idem-" + suffix;

        var first = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Stable", actor));

        jdbcTemplate.update(
                """
                DELETE FROM idempotency.idempotency_records
                WHERE operation_code = ? AND idempotency_key = ?
                """,
                IdempotencyService.IDENTITY_ISSUE_OPERATION,
                key);

        var replay = issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Stable", actor));

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.identityId()).isEqualTo(first.identityId());
        assertThat(replay.canonicalKsNumber()).isEqualTo(first.canonicalKsNumber());

        Integer identityCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity.ks_identities WHERE issuance_request_key = ?",
                Integer.class,
                key);
        assertThat(identityCount).isEqualTo(1);
    }

    @Test
    void rollbackLeavesNoPartialIdentityState() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        String key = "issue-rollback-" + suffix;
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                    issuanceService.issue(new IssueKsIdentityCommand(key, IdentityType.TEST, "Rollback", actor));
                    throw new IllegalStateException("forced rollback");
                }))
                .isInstanceOf(IllegalStateException.class);

        Integer identityCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity.ks_identities WHERE issuance_request_key = ?",
                Integer.class,
                key);
        assertThat(identityCount).isZero();
    }

    @Test
    void closedIdentityRetainsCanonicalNumber() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "issue-close-" + suffix, IdentityType.TEST, "Close", actor));

        jdbcTemplate.update(
                "UPDATE identity.ks_identities SET status = 'CLOSED', closed_at = NOW() AT TIME ZONE 'UTC' WHERE id = ?",
                issued.identityId());

        String canonical = jdbcTemplate.queryForObject(
                "SELECT canonical_ks_number FROM identity.ks_identities WHERE id = ?",
                String.class,
                issued.identityId());
        assertThat(canonical).isEqualTo(issued.canonicalKsNumber().canonicalValue());
    }
}
