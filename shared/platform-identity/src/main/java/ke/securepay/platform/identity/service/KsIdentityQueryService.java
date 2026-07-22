package ke.securepay.platform.identity.service;

import java.util.Optional;
import java.util.UUID;
import ke.securepay.platform.identity.model.KsIdentityRecord;

public interface KsIdentityQueryService {

    Optional<KsIdentityRecord> findById(UUID identityId);

    Optional<KsIdentityRecord> findByCanonicalKsNumber(String canonicalKsNumber);

    Optional<KsIdentityRecord> findByNormalizedAlias(String normalizedAlias);
}
