package ke.securepay.core.api.identity.mapper;

import ke.securepay.core.api.identity.response.IssuedKsIdentityResponse;
import ke.securepay.core.api.identity.response.RetrievedIdentityResponse;
import ke.securepay.core.api.identity.response.TransitionedIdentityResponse;
import ke.securepay.platform.identity.model.KsIdentityRecord;
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

    public static RetrievedIdentityResponse retrievedFrom(KsIdentityRecord record) {
        return new RetrievedIdentityResponse(
                record.id(),
                record.canonicalKsNumber().canonicalValue(),
                record.sequenceNumber(),
                record.identityType().name(),
                record.status().name(),
                record.displayName(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    public static TransitionedIdentityResponse from(KsIdentityRecord record) {
        return new TransitionedIdentityResponse(
                record.id(),
                record.canonicalKsNumber().canonicalValue(),
                record.status().name(),
                record.updatedAt()
        );
    }
}
