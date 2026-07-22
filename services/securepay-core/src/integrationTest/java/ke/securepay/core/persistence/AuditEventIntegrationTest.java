package ke.securepay.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneOffset;
import java.util.Map;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.persistence.audit.AuditEventRepository;
import ke.securepay.platform.persistence.audit.AuditCategory;
import ke.securepay.platform.persistence.audit.AuditWriter;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SecurePayIntegrationTest
class AuditEventIntegrationTest {

    @Autowired
    private AuditWriter auditWriter;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    @Transactional
    void auditEventsCanBeAppendedWithActorAndCorrelationContext() {
        ActorContext actor = ActorContextFactory.withCorrelation(
                ActorContextFactory.test("securepay-core"),
                "req_audit_test_001",
                "corr_audit_test_001");

        var record = auditWriter.append(
                AuditCategory.PLATFORM_TECHNICAL,
                "platform.technical.test.audit",
                actor,
                "technical_test",
                "audit-key-1",
                "create",
                null,
                Map.of("status", "created"),
                "integration test",
                Map.of("phase", "3"));

        var loaded = auditEventRepository.findByEventId(record.eventId()).orElseThrow();
        assertThat(loaded.requestId()).isEqualTo("req_audit_test_001");
        assertThat(loaded.correlationId()).isEqualTo("corr_audit_test_001");
        assertThat(loaded.actorType()).isEqualTo("service");
        assertThat(loaded.newState()).containsEntry("status", "created");
        assertThat(loaded.occurredAt().atZone(ZoneOffset.UTC).getOffset().getTotalSeconds()).isZero();
    }

    @Test
    void auditEventsRejectForbiddenPayloadKeys() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        assertThatThrownBy(() -> auditWriter.append(
                        AuditCategory.PLATFORM_TECHNICAL,
                        "platform.technical.test.audit",
                        actor,
                        "technical_test",
                        "audit-key-2",
                        "create",
                        null,
                        Map.of("password", "secret"),
                        null,
                        Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void auditEventsCannotBeUpdatedAtDatabaseLevel() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        var record = auditWriter.append(
                AuditCategory.PLATFORM_TECHNICAL,
                "platform.technical.test.audit",
                actor,
                "technical_test",
                "audit-key-3",
                "create",
                null,
                Map.of("status", "created"),
                null,
                Map.of());

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE audit.audit_events SET action = 'mutate' WHERE event_id = ?",
                        record.eventId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void auditEventsCannotBeDeletedAtDatabaseLevel() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        var record = auditWriter.append(
                AuditCategory.PLATFORM_TECHNICAL,
                "platform.technical.test.audit",
                actor,
                "technical_test",
                "audit-key-4",
                "create",
                null,
                Map.of("status", "created"),
                null,
                Map.of());

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit.audit_events WHERE event_id = ?", record.eventId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }
}
