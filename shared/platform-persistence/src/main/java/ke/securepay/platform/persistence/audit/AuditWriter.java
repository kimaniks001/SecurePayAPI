package ke.securepay.platform.persistence.audit;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import ke.securepay.platform.common.ids.IdentifierRules;
import ke.securepay.platform.observability.metrics.PersistenceMetricNames;
import ke.securepay.platform.persistence.actor.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditWriter.class);

    private final AuditEventRepository repository;
    private final AuditPayloadValidator payloadValidator;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public AuditWriter(
            AuditEventRepository repository,
            AuditPayloadValidator payloadValidator,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.payloadValidator = payloadValidator;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public AuditEventRecord append(
            AuditCategory category,
            String eventType,
            ActorContext actor,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> previousState,
            Map<String, Object> newState,
            String reason,
            Map<String, Object> metadata) {
        try {
            payloadValidator.sanitize(previousState);
            payloadValidator.sanitize(newState);
            payloadValidator.sanitize(metadata);

            Instant occurredAt = clock.instant();
            AuditEventRecord record = new AuditEventRecord(
                    UUID.randomUUID(),
                    IdentifierRules.newRequestId(),
                    category.value(),
                    eventType,
                    actor.actorType().apiValue(),
                    actor.actorId(),
                    actor.actorKsNumber(),
                    actor.applicationId(),
                    resourceType,
                    resourceId,
                    action,
                    previousState,
                    newState,
                    reason,
                    actor.requestId(),
                    actor.correlationId(),
                    actor.sourceService(),
                    occurredAt,
                    occurredAt,
                    metadata == null ? Map.of() : metadata,
                    1,
                    null);

            repository.append(record);
            meterRegistry.counter(PersistenceMetricNames.AUDIT_EVENTS_APPENDED).increment();
            return record;
        } catch (RuntimeException ex) {
            log.error("audit_append_failed eventType={} resourceType={}", eventType, resourceType);
            throw ex;
        }
    }
}
