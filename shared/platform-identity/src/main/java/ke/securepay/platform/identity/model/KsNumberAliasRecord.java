package ke.securepay.platform.identity.model;

import java.time.Instant;
import java.util.UUID;

/** Internal immutable alias record mapped from persistence. */
public record KsNumberAliasRecord(
        UUID id,
        UUID identityId,
        String alias,
        String normalizedAlias,
        AliasType aliasType,
        AliasStatus status,
        boolean primaryDisplayAlias,
        String createdByActorType,
        String createdByActorId,
        String requestId,
        String correlationId,
        Instant createdAt,
        Instant updatedAt,
        Instant releasedAt,
        long version) {}
