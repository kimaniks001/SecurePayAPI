package ke.securepay.platform.persistence.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxEventRecord(
        UUID id,
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String eventVersion,
        Map<String, Object> payload,
        Map<String, Object> metadata,
        String correlationId,
        String causationId,
        String actorType,
        String actorId,
        String sourceService,
        OutboxStatus status,
        Instant availableAt,
        int attemptCount,
        Instant lastAttemptAt,
        Instant publishedAt,
        String failureReason,
        Instant createdAt,
        long version) {}
