package ke.securepay.platform.persistence.idempotency;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IdempotencyRecord(
        UUID id,
        String applicationId,
        String actorId,
        String idempotencyKey,
        String operationCode,
        String requestHash,
        String requestContentType,
        String resourceType,
        String resourceId,
        IdempotencyStatus processingStatus,
        Integer responseStatus,
        String responseContentType,
        Map<String, Object> responseBody,
        String failureCode,
        Instant lockedUntil,
        Instant expiresAt,
        Instant createdAt,
        Instant completedAt,
        Instant updatedAt,
        long version) {}
