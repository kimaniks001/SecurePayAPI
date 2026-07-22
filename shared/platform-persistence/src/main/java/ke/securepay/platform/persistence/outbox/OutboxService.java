package ke.securepay.platform.persistence.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import ke.securepay.platform.common.ids.IdentifierRules;
import ke.securepay.platform.observability.metrics.PersistenceMetricNames;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.events.DomainEventEnvelope;
import ke.securepay.platform.persistence.events.TechnicalEventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    private static final int MAX_DEAD_LETTER_ATTEMPTS = 5;

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public OutboxService(
            OutboxRepository repository, ObjectMapper objectMapper, Clock clock, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public OutboxEventRecord appendTechnicalTestCreated(
            ActorContext actor, String aggregateId, Map<String, Object> data, String causationId) {
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                IdentifierRules.newRequestId(),
                TechnicalEventTypes.PLATFORM_TEST_CREATED,
                TechnicalEventTypes.EVENT_VERSION,
                clock.instant(),
                actor.correlationId(),
                causationId,
                actor.sourceService(),
                new DomainEventEnvelope.Actor(actor.actorType().apiValue(), actor.actorId(), actor.actorKsNumber()),
                new DomainEventEnvelope.Resource("technical_test", aggregateId, "1"),
                data,
                Map.of("phase", "3"));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.convertValue(envelope, Map.class);

        OutboxEventRecord record = new OutboxEventRecord(
                UUID.randomUUID(),
                envelope.event_id(),
                "technical_test",
                aggregateId,
                envelope.event_type(),
                envelope.event_version(),
                payload,
                Map.of("phase", "3"),
                actor.correlationId(),
                causationId,
                actor.actorType().apiValue(),
                actor.actorId(),
                actor.sourceService(),
                OutboxStatus.PENDING,
                clock.instant(),
                0,
                null,
                null,
                null,
                clock.instant(),
                0L);

        repository.insert(record);
        return record;
    }

    @Transactional
    public OutboxEventRecord markProcessing(UUID id, long expectedVersion) {
        repository.markProcessing(id, expectedVersion);
        return reload(id);
    }

    @Transactional
    public OutboxEventRecord markPublished(UUID id, long expectedVersion) {
        repository.markPublished(id, expectedVersion);
        meterRegistry.counter(PersistenceMetricNames.OUTBOX_PUBLISHED).increment();
        return reload(id);
    }

    @Transactional
    public OutboxEventRecord markFailed(UUID id, long expectedVersion, String failureReason) {
        repository.markFailed(id, expectedVersion, failureReason);
        meterRegistry.counter(PersistenceMetricNames.OUTBOX_FAILED).increment();
        OutboxEventRecord updated = reload(id);
        if (updated.attemptCount() >= MAX_DEAD_LETTER_ATTEMPTS) {
            return markDeadLetter(id, updated.version(), failureReason);
        }
        return updated;
    }

    @Transactional
    public OutboxEventRecord markDeadLetter(UUID id, long expectedVersion, String failureReason) {
        repository.markDeadLetter(id, expectedVersion, failureReason);
        meterRegistry.counter(PersistenceMetricNames.OUTBOX_DEAD_LETTER).increment();
        log.warn("outbox_dead_letter id={} reason={}", id, failureReason);
        return reload(id);
    }

    public OutboxEventRecord findByEventId(String eventId) {
        return repository.findByEventId(eventId).orElseThrow();
    }

    private OutboxEventRecord reload(UUID id) {
        return repository.findById(id).orElseThrow();
    }
}
