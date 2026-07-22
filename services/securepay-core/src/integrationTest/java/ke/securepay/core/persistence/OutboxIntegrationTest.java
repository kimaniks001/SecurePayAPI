package ke.securepay.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.persistence.events.TechnicalEventTypes;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import ke.securepay.platform.persistence.outbox.OutboxEventRecord;
import ke.securepay.platform.persistence.outbox.OutboxRepository;
import ke.securepay.platform.persistence.outbox.OutboxService;
import ke.securepay.platform.persistence.outbox.OutboxStatus;
import ke.securepay.platform.persistence.technical.TechnicalFoundationService;
import ke.securepay.platform.testing.contracts.EventEnvelopeSchemaSupport;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SecurePayIntegrationTest
class OutboxIntegrationTest {

    private static JsonSchema eventSchema;

    @Autowired
    private TechnicalFoundationService technicalFoundationService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
        eventSchema = EventEnvelopeSchemaSupport.loadSchema();
    }

    @Test
    void outboxPersistsInSameTransactionAsTechnicalStateChange() throws Exception {
        String suffix = UUID.randomUUID().toString();
        ActorContext actor = ActorContextFactory.withCorrelation(
                ActorContextFactory.test("securepay-core"), "req_outbox_" + suffix, "corr_outbox_" + suffix);
        String recordKey = "outbox-record-" + suffix;

        var result = technicalFoundationService.recordTechnicalCreation(actor, recordKey, Map.of("phase", "3"));

        assertThat(technicalFoundationService.technicalRecordExists(recordKey)).isTrue();
        OutboxEventRecord outbox = outboxRepository.findByEventId(result.outboxEvent().eventId()).orElseThrow();
        assertThat(outbox.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.correlationId()).isEqualTo("corr_outbox_" + suffix);
        assertThat(outbox.actorType()).isEqualTo("service");

        String persistedPayloadJson = jdbcTemplate.queryForObject(
                "SELECT payload::text FROM events.outbox_events WHERE event_id = ?",
                String.class,
                outbox.eventId());
        assertThat(persistedPayloadJson).isNotBlank();
        assertThat(persistedPayloadJson).doesNotContain("ke.securepay");

        JsonNode persistedPayload = objectMapper.readTree(persistedPayloadJson);
        Set<ValidationMessage> errors = eventSchema.validate(persistedPayload);
        assertThat(errors).isEmpty();
        assertThat(persistedPayload.get("event_type").asText()).isEqualTo(TechnicalEventTypes.PLATFORM_TEST_CREATED);
        assertThat(persistedPayload.get("event_version").asText()).isEqualTo(TechnicalEventTypes.EVENT_VERSION);
        assertThat(persistedPayload.get("correlation_id").asText()).isEqualTo("corr_outbox_" + suffix);
        assertThat(persistedPayload.get("actor").get("type").asText()).isEqualTo("service");
        assertThat(persistedPayload.get("actor").get("id").asText()).isEqualTo("test-actor");
    }

    @Test
    void rollbackRemovesTechnicalRecordAndOutboxEvent() {
        String suffix = UUID.randomUUID().toString();
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String recordKey = "outbox-rollback-" + suffix;
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                    technicalFoundationService.recordTechnicalCreation(actor, recordKey, Map.of("phase", "3"));
                    throw new IllegalStateException("forced rollback for technical flow");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forced rollback");

        assertThat(technicalFoundationService.technicalRecordExists(recordKey)).isFalse();
        assertThat(technicalFoundationService.countOutboxEventsForAggregate(recordKey)).isZero();
    }

    @Test
    void eventIdMustBeUnique() {
        String suffix = UUID.randomUUID().toString();
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-unique-" + suffix, Map.of("phase", "3"), null);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO events.outbox_events (
                            id, event_id, aggregate_type, aggregate_id, event_type, event_version,
                            payload, metadata, correlation_id, actor_type, source_service, status, version
                        ) VALUES (
                            gen_random_uuid(), ?, 'technical_test', ?, 'platform.test.created', '1.0.0',
                            '{}'::jsonb, '{}'::jsonb, ?, 'service', 'securepay-core', 'PENDING', 0
                        )
                        """,
                        created.eventId(),
                        "dup-" + suffix,
                        "corr_dup_" + suffix))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void outboxStatusTransitionsRetainPublicationMetadata() {
        String suffix = UUID.randomUUID().toString();
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-transition-" + suffix, Map.of("phase", "3"), null);

        OutboxEventRecord processing = outboxService.markProcessing(created.id(), created.version());
        assertThat(processing.attemptCount()).isEqualTo(1);
        assertThat(processing.status()).isEqualTo(OutboxStatus.PROCESSING);

        OutboxEventRecord published = outboxService.markPublished(processing.id(), processing.version());
        assertThat(published.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(published.publishedAt()).isNotNull();
    }

    @Test
    void deadLetterTransitionIsControlled() {
        String suffix = UUID.randomUUID().toString();
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-dead-letter-" + suffix, Map.of("phase", "3"), null);

        OutboxEventRecord deadLetter = outboxService.markDeadLetter(created.id(), created.version(), "technical test failure");
        assertThat(deadLetter.status()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(deadLetter.failureReason()).isEqualTo("technical test failure");
    }

    @Test
    void optimisticLockConflictOnOutboxUpdateFails() {
        String suffix = UUID.randomUUID().toString();
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-optimistic-" + suffix, Map.of("phase", "3"), null);

        assertThatThrownBy(() -> outboxRepository.markPublished(created.id(), created.version() + 99))
                .isInstanceOf(OptimisticLockException.class);
    }
}
