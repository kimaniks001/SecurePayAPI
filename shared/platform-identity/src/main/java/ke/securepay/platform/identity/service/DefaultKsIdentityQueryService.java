package ke.securepay.platform.identity.service;

import java.util.Optional;
import java.util.UUID;
import ke.securepay.platform.identity.model.KsIdentityRecord;
import ke.securepay.platform.identity.persistence.KsIdentityRepository;
import ke.securepay.platform.identity.persistence.KsNumberAliasRepository;
import org.springframework.stereotype.Service;

@Service
public class DefaultKsIdentityQueryService implements KsIdentityQueryService {

    private final KsIdentityRepository identityRepository;
    private final KsNumberAliasRepository aliasRepository;

    public DefaultKsIdentityQueryService(
            KsIdentityRepository identityRepository, KsNumberAliasRepository aliasRepository) {
        this.identityRepository = identityRepository;
        this.aliasRepository = aliasRepository;
    }

    @Override
    public Optional<KsIdentityRecord> findById(UUID identityId) {
        return identityRepository.findById(identityId);
    }

    @Override
    public Optional<KsIdentityRecord> findByCanonicalKsNumber(String canonicalKsNumber) {
        return identityRepository.findByCanonicalKsNumber(canonicalKsNumber);
    }

    @Override
    public Optional<KsIdentityRecord> findByNormalizedAlias(String normalizedAlias) {
        return aliasRepository
                .findByNormalizedAlias(normalizedAlias.toLowerCase())
                .flatMap(alias -> identityRepository.findById(alias.identityId()));
    }
}
