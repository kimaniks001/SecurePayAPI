package ke.securepay.core.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.command.LifecycleTransitionCommand;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import ke.securepay.platform.identity.exception.IdentityLifecycleException;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import ke.securepay.platform.identity.service.KsIdentityLifecycleService;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SecurePayIntegrationTest
class KsIdentityLifecycleIntegrationTest {

    @Autowired
    private KsIdentityIssuanceService issuanceService;

    @Autowired
    private KsIdentityLifecycleService lifecycleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void allowedLifecycleTransitionsSucceed() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "life-" + suffix, IdentityType.TEST, "Life", actor));

        var active = lifecycleService.transition(new LifecycleTransitionCommand(
                issued.identityId(), IdentityStatus.ACTIVE, "activate", actor));
        assertThat(active.status()).isEqualTo(IdentityStatus.ACTIVE);

        var suspended = lifecycleService.transition(new LifecycleTransitionCommand(
                issued.identityId(), IdentityStatus.SUSPENDED, "suspend", actor));
        assertThat(suspended.status()).isEqualTo(IdentityStatus.SUSPENDED);

        var reactivated = lifecycleService.transition(new LifecycleTransitionCommand(
                issued.identityId(), IdentityStatus.ACTIVE, "reactivate", actor));
        assertThat(reactivated.status()).isEqualTo(IdentityStatus.ACTIVE);

        var closed = lifecycleService.transition(new LifecycleTransitionCommand(
                issued.identityId(), IdentityStatus.CLOSED, "close", actor));
        assertThat(closed.status()).isEqualTo(IdentityStatus.CLOSED);
    }

    @Test
    void closedIdentityCannotReactivate() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "life-closed-" + suffix, IdentityType.TEST, "Closed", actor));
        lifecycleService.transition(new LifecycleTransitionCommand(
                issued.identityId(), IdentityStatus.ACTIVE, "activate", actor));
        lifecycleService.transition(new LifecycleTransitionCommand(
                issued.identityId(), IdentityStatus.CLOSED, "close", actor));

        assertThatThrownBy(() -> lifecycleService.transition(new LifecycleTransitionCommand(
                        issued.identityId(), IdentityStatus.ACTIVE, "illegal", actor)))
                .isInstanceOf(IdentityLifecycleException.class);
    }

    @Test
    void lifecyclePersistsAuditAndOutboxAndPreservesCanonicalNumber() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "life-audit-" + suffix, IdentityType.TEST, "Audit", actor));

        lifecycleService.transition(new LifecycleTransitionCommand(
                issued.identityId(), IdentityStatus.ACTIVE, "activate", actor));

        String canonical = jdbcTemplate.queryForObject(
                "SELECT canonical_ks_number FROM identity.ks_identities WHERE id = ?",
                String.class,
                issued.identityId());
        Long sequence = jdbcTemplate.queryForObject(
                "SELECT sequence_number FROM identity.ks_identities WHERE id = ?",
                Long.class,
                issued.identityId());

        assertThat(canonical).isEqualTo(issued.canonicalKsNumber().canonicalValue());
        assertThat(sequence).isEqualTo(issued.sequenceNumber());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit.audit_events WHERE event_type = ? AND resource_id = ?",
                Integer.class,
                IdentityEventTypes.STATUS_CHANGED,
                issued.identityId().toString());
        assertThat(auditCount).isGreaterThanOrEqualTo(1);

        Integer outboxCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM events.outbox_events
                WHERE aggregate_id = ? AND event_type = ?
                """,
                Integer.class,
                issued.identityId().toString(),
                IdentityEventTypes.STATUS_CHANGED);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void lifecycleRollbackRemovesTransitionChanges() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "life-rollback-" + suffix, IdentityType.TEST, "Rollback", actor));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                    lifecycleService.transition(new LifecycleTransitionCommand(
                            issued.identityId(), IdentityStatus.ACTIVE, "activate", actor));
                    throw new IllegalStateException("forced rollback");
                }))
                .isInstanceOf(IllegalStateException.class);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM identity.ks_identities WHERE id = ?",
                String.class,
                issued.identityId());
        assertThat(status).isEqualTo("PENDING");
    }

    @Test
    void optimisticLockConflictFailsSafely() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "life-lock-" + suffix, IdentityType.TEST, "Lock", actor));

        jdbcTemplate.update("UPDATE identity.ks_identities SET version = version + 1 WHERE id = ?", issued.identityId());

        assertThatThrownBy(() -> lifecycleService.transition(new LifecycleTransitionCommand(
                        issued.identityId(), IdentityStatus.ACTIVE, "activate", actor)))
                .isInstanceOf(OptimisticLockException.class);
    }
}
