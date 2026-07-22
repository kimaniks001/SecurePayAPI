package ke.securepay.platform.identity.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ke.securepay.platform.identity.alias.AliasNormalizer;
import ke.securepay.platform.identity.command.AliasLifecycleTransitionCommand;
import ke.securepay.platform.identity.command.CreateAliasCommand;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import ke.securepay.platform.identity.exception.IdentityLifecycleException;
import ke.securepay.platform.identity.exception.IdentityNotFoundException;
import ke.securepay.platform.identity.metrics.IdentityMetricNames;
import ke.securepay.platform.identity.model.AliasStatus;
import ke.securepay.platform.identity.model.KsNumberAliasRecord;
import ke.securepay.platform.identity.outbox.IdentityOutboxWriter;
import ke.securepay.platform.identity.persistence.KsIdentityRepository;
import ke.securepay.platform.identity.persistence.KsNumberAliasRepository;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.audit.AuditCategory;
import ke.securepay.platform.persistence.audit.AuditWriter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultKsAliasService implements KsAliasService {

    private static final Map<AliasStatus, Set<AliasStatus>> ALLOWED = Map.of(
            AliasStatus.RESERVED, Set.of(AliasStatus.ACTIVE, AliasStatus.RETIRED),
            AliasStatus.ACTIVE, Set.of(AliasStatus.SUSPENDED, AliasStatus.RETIRED),
            AliasStatus.SUSPENDED, Set.of(AliasStatus.ACTIVE, AliasStatus.RETIRED));

    private final KsIdentityRepository identityRepository;
    private final KsNumberAliasRepository aliasRepository;
    private final AuditWriter auditWriter;
    private final IdentityOutboxWriter outboxWriter;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public DefaultKsAliasService(
            KsIdentityRepository identityRepository,
            KsNumberAliasRepository aliasRepository,
            AuditWriter auditWriter,
            IdentityOutboxWriter outboxWriter,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.identityRepository = identityRepository;
        this.aliasRepository = aliasRepository;
        this.auditWriter = auditWriter;
        this.outboxWriter = outboxWriter;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public KsNumberAliasRecord createAlias(CreateAliasCommand command) {
        var identity = identityRepository
                .findById(command.identityId())
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found"));

        var normalized = AliasNormalizer.normalizeOrThrow(command.alias());
        var now = clock.instant();
        UUID aliasId = UUID.randomUUID();
        ActorContext actor = command.actorContext();

        KsNumberAliasRecord record = new KsNumberAliasRecord(
                aliasId,
                identity.id(),
                normalized.rawAlias(),
                normalized.normalizedAlias(),
                command.aliasType(),
                AliasStatus.RESERVED,
                command.primaryDisplayAlias(),
                actor.actorType().apiValue(),
                actor.actorId(),
                actor.requestId(),
                actor.correlationId(),
                now,
                now,
                null,
                0L);

        try {
            aliasRepository.insert(record);
        } catch (DuplicateKeyException ex) {
            meterRegistry.counter(IdentityMetricNames.IDENTITY_ALIAS_CONFLICT).increment();
            throw ex;
        }

        var audit = auditWriter.append(
                AuditCategory.IDENTITY,
                IdentityEventTypes.ALIAS_CREATED,
                actor,
                "identity_alias",
                aliasId.toString(),
                "create",
                null,
                Map.of(
                        "alias", normalized.rawAlias(),
                        "normalized_alias", normalized.normalizedAlias(),
                        "identity_id", identity.id().toString()),
                "Alias created",
                Map.of("canonical_ks_number", identity.canonicalKsNumber().canonicalValue()));

        outboxWriter.append(
                IdentityEventTypes.ALIAS_CREATED,
                actor,
                identity.id().toString(),
                Map.of(
                        "alias_id", aliasId.toString(),
                        "identity_id", identity.id().toString(),
                        "canonical_ks_number", identity.canonicalKsNumber().canonicalValue(),
                        "alias", normalized.rawAlias(),
                        "normalized_alias", normalized.normalizedAlias()),
                audit.eventId());

        meterRegistry.counter(IdentityMetricNames.IDENTITY_ALIAS_CREATED).increment();
        return record;
    }

    @Override
    @Transactional
    public KsNumberAliasRecord transitionAlias(AliasLifecycleTransitionCommand command) {
        KsNumberAliasRecord current = aliasRepository
                .findById(command.aliasId())
                .orElseThrow(() -> new IdentityNotFoundException("Alias not found"));

        AliasStatus target = command.targetStatus();
        Set<AliasStatus> allowed = ALLOWED.getOrDefault(current.status(), Set.of());
        if (!allowed.contains(target)) {
            throw new IdentityLifecycleException("Alias transition not allowed: " + current.status() + " -> " + target);
        }

        var now = clock.instant();
        var releasedAt = target == AliasStatus.RETIRED ? now : current.releasedAt();
        aliasRepository.updateStatus(current.id(), current.version(), target, now, releasedAt);

        ActorContext actor = command.actorContext();
        var audit = auditWriter.append(
                AuditCategory.IDENTITY,
                IdentityEventTypes.ALIAS_STATUS_CHANGED,
                actor,
                "identity_alias",
                current.id().toString(),
                "status_change",
                Map.of("status", current.status().name()),
                Map.of("status", target.name()),
                command.reason(),
                Map.of("normalized_alias", current.normalizedAlias()));

        outboxWriter.append(
                IdentityEventTypes.ALIAS_STATUS_CHANGED,
                actor,
                current.identityId().toString(),
                Map.of(
                        "alias_id", current.id().toString(),
                        "identity_id", current.identityId().toString(),
                        "normalized_alias", current.normalizedAlias(),
                        "previous_status", current.status().name(),
                        "new_status", target.name()),
                audit.eventId());

        return aliasRepository.findById(current.id()).orElseThrow();
    }
}
