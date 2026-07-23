package ke.securepay.core.api.identity.response;

import java.time.Instant;
import java.util.UUID;

public record RetrievedIdentityResponse(
        UUID identityId,
        String canonicalKsNumber,
        long sequenceNumber,
        String identityType,
        String status,
        String displayName,
        Instant createdAt,
        Instant updatedAt
) {}
