package ke.securepay.platform.persistence.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventRecord(
        UUID id,
        String eventId,
        String category,
        String eventType,
        String actorType,
        String actorId,
        String actorKsNumber,
        String applicationId,
        String resourceType,
        String resourceId,
        String action,
        Map<String, Object> previousState,
        Map<String, Object> newState,
        String reason,
        String requestId,
        String correlationId,
        String sourceService,
        Instant occurredAt,
        Instant createdAt,
        Map<String, Object> metadata,
        int integrityVersion,
        String integrityHash) {}
