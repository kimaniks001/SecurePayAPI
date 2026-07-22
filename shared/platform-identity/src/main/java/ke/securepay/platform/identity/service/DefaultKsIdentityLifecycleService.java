package ke.securepay.platform.identity.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import ke.securepay.platform.identity.command.LifecycleTransitionCommand;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import ke.securepay.platform.identity.exception.IdentityLifecycleException;
import ke.securepay.platform.identity.exception.IdentityNotFoundException;
import ke.securepay.platform.identity.metrics.IdentityMetricNames;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.model.KsIdentityRecord;
import ke.securepay.platform.identity.outbox.IdentityOutboxWriter;
import ke.securepay.platform.identity.persistence.KsIdentityRepository;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.audit.AuditCategory;
import ke.securepay.platform.persistence.audit.AuditWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultKsIdentityLifecycleService implements KsIdentityLifecycleService {

    private static final Map<IdentityStatus, Set<IdentityStatus>> ALLOWED = Map.of(
            IdentityStatus.PENDING, Set.of(IdentityStatus.ACTIVE),
            IdentityStatus.ACTIVE, Set.of(IdentityStatus.SUSPENDED, IdentityStatus.CLOSED),
            IdentityStatus.SUSPENDED, Set.of(IdentityStatus.ACTIVE, IdentityStatus.CLOSED));

    private final KsIdentityRepository identityRepository;
    private final AuditWriter auditWriter;
    private final IdentityOutboxWriter outboxWriter;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public DefaultKsIdentityLifecycleService(
            KsIdentityRepository identityRepository,
            AuditWriter auditWriter,
            IdentityOutboxWriter outboxWriter,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.identityRepository = identityRepository;
        this.auditWriter = auditWriter;
        this.outboxWriter = outboxWriter;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public KsIdentityRecord transition(LifecycleTransitionCommand command) {
        KsIdentityRecord current = identityRepository
                .findById(command.identityId())
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found"));

        IdentityStatus target = command.targetStatus();
        Set<IdentityStatus> allowedTargets = ALLOWED.getOrDefault(current.status(), Set.of());
        if (!allowedTargets.contains(target)) {
            throw new IdentityLifecycleException(
                    "Transition not allowed: " + current.status() + " -> " + target);
        }

        var now = clock.instant();
        var suspendedAt = target == IdentityStatus.SUSPENDED ? now : current.suspendedAt();
        var closedAt = target == IdentityStatus.CLOSED ? now : current.closedAt();

        identityRepository.updateStatus(current.id(), current.version(), target, now, suspendedAt, closedAt);

        ActorContext actor = command.actorContext();
        Map<String, Object> previous = Map.of("status", current.status().name());
        Map<String, Object> next = Map.of("status", target.name());

        var audit = auditWriter.append(
                AuditCategory.IDENTITY,
                IdentityEventTypes.STATUS_CHANGED,
                actor,
                "identity",
                current.id().toString(),
                "status_change",
                previous,
                next,
                command.reason(),
                Map.of("canonical_ks_number", current.canonicalKsNumber().canonicalValue()));

        outboxWriter.append(
                IdentityEventTypes.STATUS_CHANGED,
                actor,
                current.id().toString(),
                Map.of(
                        "identity_id", current.id().toString(),
                        "canonical_ks_number", current.canonicalKsNumber().canonicalValue(),
                        "previous_status", current.status().name(),
                        "new_status", target.name()),
                audit.eventId());

        meterRegistry.counter(IdentityMetricNames.IDENTITY_STATUS_CHANGED).increment();

        return identityRepository.findById(current.id()).orElseThrow();
    }
}
