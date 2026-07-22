package ke.securepay.core.api.identity.mapper;

import ke.securepay.core.api.identity.response.IssuedKsIdentityResponse;
import ke.securepay.platform.identity.result.IssuedKsIdentityResult;

public final class IdentityResponseMapper {

    private IdentityResponseMapper() {}

    public static IssuedKsIdentityResponse from(IssuedKsIdentityResult result) {
        return new IssuedKsIdentityResponse(
                result.identityId(),
                result.canonicalKsNumber().canonicalValue(),
                result.sequenceNumber(),
                result.identityType().name(),
                result.status().name(),
                result.replayed()
        );
    }
}
