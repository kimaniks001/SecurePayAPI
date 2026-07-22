package ke.securepay.platform.identity.result;

import java.util.UUID;
import ke.securepay.platform.identity.ksnumber.KsNumber;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.model.IdentityType;

public record IssuedKsIdentityResult(
        UUID identityId,
        KsNumber canonicalKsNumber,
        long sequenceNumber,
        IdentityType identityType,
        IdentityStatus status,
        boolean replayed) {}
