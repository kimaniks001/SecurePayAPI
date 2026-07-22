package ke.securepay.platform.identity.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import ke.securepay.platform.common.ids.IdentifierRules;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.events.DomainEventEnvelope;
import ke.securepay.platform.persistence.outbox.OutboxEventRecord;
import ke.securepay.platform.persistence.outbox.OutboxRepository;
import ke.securepay.platform.persistence.outbox.OutboxStatus;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityOutboxWriter {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdentityOutboxWriter(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public OutboxEventRecord append(
            String eventType,
            ActorContext actor,
            String aggregateId,
            Map<String, Object> data,
            String causationId) {
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                IdentifierRules.newRequestId(),
                eventType,
                IdentityEventTypes.EVENT_VERSION,
                clock.instant(),
                actor.correlationId(),
                causationId,
                actor.sourceService(),
                new DomainEventEnvelope.Actor(actor.actorType().apiValue(), actor.actorId(), actor.actorKsNumber()),
                new DomainEventEnvelope.Resource("identity", aggregateId, "1"),
                data,
                Map.of("phase", "4"));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.convertValue(envelope, Map.class);

        OutboxEventRecord record = new OutboxEventRecord(
                UUID.randomUUID(),
                envelope.event_id(),
                "identity",
                aggregateId,
                envelope.event_type(),
                envelope.event_version(),
                payload,
                Map.of("phase", "4"),
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
}
