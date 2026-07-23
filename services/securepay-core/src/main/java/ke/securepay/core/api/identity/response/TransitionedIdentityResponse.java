package ke.securepay.core.api.identity.response;

import java.time.Instant;
import java.util.UUID;

public record TransitionedIdentityResponse(
        UUID identityId,
        String canonicalKsNumber,
        String status,
        Instant updatedAt
) {}
