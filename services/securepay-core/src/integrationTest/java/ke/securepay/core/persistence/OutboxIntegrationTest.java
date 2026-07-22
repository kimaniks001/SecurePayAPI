package ke.securepay.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import ke.securepay.core.support.IntegrationTestContainersConfig;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import ke.securepay.platform.persistence.outbox.OutboxEventRecord;
import ke.securepay.platform.persistence.outbox.OutboxRepository;
import ke.securepay.platform.persistence.outbox.OutboxService;
import ke.securepay.platform.persistence.outbox.OutboxStatus;
import ke.securepay.platform.persistence.technical.TechnicalFoundationService;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestContainersConfig.class)
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
    static void enforceDockerPolicy() throws Exception {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
        String schemaJson = Files.readString(Path.of("contracts/events/event-envelope-v1.schema.json"));
        eventSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);
    }

    @Test
    void outboxPersistsInSameTransactionAsTechnicalStateChange() throws Exception {
        ActorContext actor = ActorContextFactory.withCorrelation(
                ActorContextFactory.test("securepay-core"), "req_outbox_001", "corr_outbox_001");
        String recordKey = "outbox-record-001";

        var result = technicalFoundationService.recordTechnicalCreation(actor, recordKey, Map.of("phase", "3"));

        assertThat(technicalFoundationService.technicalRecordExists(recordKey)).isTrue();
        OutboxEventRecord outbox = outboxRepository.findByEventId(result.outboxEvent().eventId()).orElseThrow();
        assertThat(outbox.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.correlationId()).isEqualTo("corr_outbox_001");
        assertThat(outbox.actorType()).isEqualTo("service");

        ObjectMapper validatorMapper = objectMapper.copy().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Set<ValidationMessage> errors = eventSchema.validate(validatorMapper.valueToTree(outbox.payload()));
        assertThat(errors).isEmpty();
    }

    @Test
    void rollbackRemovesTechnicalRecordAndOutboxEvent() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        String recordKey = "outbox-rollback-001";
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            technicalFoundationService.recordTechnicalCreation(actor, recordKey, Map.of("phase", "3"));
            status.setRollbackOnly();
        });

        assertThat(technicalFoundationService.technicalRecordExists(recordKey)).isFalse();
        assertThat(technicalFoundationService.countOutboxEventsForAggregate(recordKey)).isZero();
    }

    @Test
    void eventIdMustBeUnique() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-unique-001", Map.of("phase", "3"), null);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO events.outbox_events (
                            id, event_id, aggregate_type, aggregate_id, event_type, event_version,
                            payload, metadata, correlation_id, actor_type, source_service, status, version
                        ) VALUES (
                            gen_random_uuid(), ?, 'technical_test', 'dup', 'platform.test.created', '1.0.0',
                            '{}'::jsonb, '{}'::jsonb, 'corr_dup', 'service', 'securepay-core', 'PENDING', 0
                        )
                        """,
                        created.eventId()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void outboxStatusTransitionsRetainPublicationMetadata() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-transition-001", Map.of("phase", "3"), null);

        OutboxEventRecord processing = outboxService.markProcessing(created.id(), created.version());
        assertThat(processing.attemptCount()).isEqualTo(1);
        assertThat(processing.status()).isEqualTo(OutboxStatus.PROCESSING);

        OutboxEventRecord published = outboxService.markPublished(processing.id(), processing.version());
        assertThat(published.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(published.publishedAt()).isNotNull();
    }

    @Test
    void deadLetterTransitionIsControlled() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-dead-letter-001", Map.of("phase", "3"), null);

        OutboxEventRecord deadLetter = outboxService.markDeadLetter(created.id(), created.version(), "technical test failure");
        assertThat(deadLetter.status()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(deadLetter.failureReason()).isEqualTo("technical test failure");
    }

    @Test
    void optimisticLockConflictOnOutboxUpdateFails() {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        OutboxEventRecord created = outboxService.appendTechnicalTestCreated(
                actor, "outbox-optimistic-001", Map.of("phase", "3"), null);

        assertThatThrownBy(() -> outboxRepository.markPublished(created.id(), created.version() + 99))
                .isInstanceOf(OptimisticLockException.class);
    }
}
