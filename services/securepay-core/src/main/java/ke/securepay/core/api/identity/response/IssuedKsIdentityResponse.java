package ke.securepay.core.api.identity.response;

import java.util.UUID;

public record IssuedKsIdentityResponse(
        UUID identityId,
        String canonicalKsNumber,
        long sequenceNumber,
        String identityType,
        String status,
        boolean replayed
) {}
