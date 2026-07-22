package ke.securepay.platform.identity.model;

import java.time.Instant;
import java.util.UUID;
import ke.securepay.platform.identity.ksnumber.KsNumber;

/** Internal immutable identity record mapped from persistence. */
public record KsIdentityRecord(
        UUID id,
        KsNumber canonicalKsNumber,
        long sequenceNumber,
        IdentityType identityType,
        IdentityStatus status,
        String displayName,
        String issuanceRequestKey,
        String issuanceRequestHash,
        String createdByActorType,
        String createdByActorId,
        String requestId,
        String correlationId,
        Instant createdAt,
        Instant updatedAt,
        Instant suspendedAt,
        Instant closedAt,
        long version) {}
