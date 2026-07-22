package ke.securepay.platform.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import ke.securepay.platform.identity.exception.IssuanceOwnershipConflictException;
import ke.securepay.platform.identity.issuance.IssuanceRequestFingerprint;
import ke.securepay.platform.identity.ksnumber.KsNumber;
import ke.securepay.platform.identity.metrics.IdentityMetricNames;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.model.KsIdentityRecord;
import ke.securepay.platform.identity.outbox.IdentityOutboxWriter;
import ke.securepay.platform.identity.persistence.KsIdentityRepository;
import ke.securepay.platform.identity.persistence.KsNumberSequenceAllocator;
import ke.securepay.platform.identity.result.IssuedKsIdentityResult;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.audit.AuditCategory;
import ke.securepay.platform.persistence.audit.AuditWriter;
import ke.securepay.platform.persistence.idempotency.IdempotencyService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultKsIdentityIssuanceService implements KsIdentityIssuanceService {

    private static final int MAX_DISPLAY_NAME_LENGTH = 128;

    private final IdempotencyService idempotencyService;
    private final KsNumberSequenceAllocator sequenceAllocator;
    private final KsIdentityRepository identityRepository;
    private final AuditWriter auditWriter;
    private final IdentityOutboxWriter outboxWriter;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public DefaultKsIdentityIssuanceService(
            IdempotencyService idempotencyService,
            KsNumberSequenceAllocator sequenceAllocator,
            KsIdentityRepository identityRepository,
            AuditWriter auditWriter,
            IdentityOutboxWriter outboxWriter,
            ObjectMapper objectMapper,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.idempotencyService = idempotencyService;
        this.sequenceAllocator = sequenceAllocator;
        this.identityRepository = identityRepository;
        this.auditWriter = auditWriter;
        this.outboxWriter = outboxWriter;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public IssuedKsIdentityResult issue(IssueKsIdentityCommand command) {
        validate(command);
        ActorContext actor = command.actorContext();
        String requestBody = toRequestBody(command);
        String requestHash = IssuanceRequestFingerprint.hash(requestBody);

        Optional<IssuedKsIdentityResult> existingOwnership =
                resolvePermanentIssuanceOwnership(command.issuanceRequestKey(), requestHash);
        if (existingOwnership.isPresent()) {
            meterRegistry.counter(IdentityMetricNames.IDENTITY_ISSUANCE_REPLAYED).increment();
            return existingOwnership.get();
        }

        var execution = idempotencyService.execute(
                actor,
                IdempotencyService.IDENTITY_ISSUE_OPERATION,
                "identity",
                command.issuanceRequestKey(),
                requestBody,
                "application/json",
                IdempotencyService.DEFAULT_LOCK,
                IdempotencyService.IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY,
                () -> createNewIdentity(command, actor, requestHash));

        if (execution.replayed()) {
            meterRegistry.counter(IdentityMetricNames.IDENTITY_ISSUANCE_REPLAYED).increment();
        }

        Map<String, Object> body = execution.record().responseBody();
        return toResult(body, execution.replayed());
    }

    private Map<String, Object> createNewIdentity(
            IssueKsIdentityCommand command, ActorContext actor, String requestHash) {
        Optional<IssuedKsIdentityResult> racedOwnership =
                resolvePermanentIssuanceOwnership(command.issuanceRequestKey(), requestHash);
        if (racedOwnership.isPresent()) {
            return toResponseMap(racedOwnership.get());
        }

        long sequenceNumber = sequenceAllocator.allocateNext();
        KsNumber canonical = KsNumber.fromSequence(sequenceNumber);
        UUID identityId = UUID.randomUUID();
        var now = clock.instant();

        KsIdentityRecord record = new KsIdentityRecord(
                identityId,
                canonical,
                sequenceNumber,
                command.identityType(),
                IdentityStatus.PENDING,
                command.displayName(),
                command.issuanceRequestKey(),
                requestHash,
                actor.actorType().apiValue(),
                actor.actorId(),
                actor.requestId(),
                actor.correlationId(),
                now,
                now,
                null,
                null,
                0L);

        try {
            identityRepository.insert(record);
        } catch (DuplicateKeyException ex) {
            KsIdentityRecord existing = identityRepository
                    .findByIssuanceRequestKey(command.issuanceRequestKey())
                    .orElseThrow(() -> ex);
            return toResponseMap(assertMatchingOwnership(existing, requestHash));
        }

        var audit = auditWriter.append(
                AuditCategory.IDENTITY,
                IdentityEventTypes.KS_NUMBER_ISSUED,
                actor,
                "identity",
                identityId.toString(),
                "issue",
                null,
                Map.of(
                        "identity_id", identityId.toString(),
                        "canonical_ks_number", canonical.canonicalValue(),
                        "sequence_number", sequenceNumber,
                        "identity_type", command.identityType().name(),
                        "status", IdentityStatus.PENDING.name()),
                "Canonical KS Number issued",
                Map.of("issuance_request_key", command.issuanceRequestKey()));

        outboxWriter.append(
                IdentityEventTypes.KS_NUMBER_ISSUED,
                actor,
                identityId.toString(),
                Map.of(
                        "identity_id", identityId.toString(),
                        "canonical_ks_number", canonical.canonicalValue(),
                        "sequence_number", sequenceNumber,
                        "identity_type", command.identityType().name(),
                        "status", IdentityStatus.PENDING.name()),
                audit.eventId());

        meterRegistry.counter(IdentityMetricNames.IDENTITY_ISSUED).increment();

        return Map.of(
                "identity_id", identityId.toString(),
                "canonical_ks_number", canonical.canonicalValue(),
                "sequence_number", sequenceNumber,
                "identity_type", command.identityType().name(),
                "status", IdentityStatus.PENDING.name());
    }

    private Optional<IssuedKsIdentityResult> resolvePermanentIssuanceOwnership(
            String issuanceRequestKey, String requestHash) {
        return identityRepository.findByIssuanceRequestKey(issuanceRequestKey).map(record -> {
            assertMatchingOwnership(record, requestHash);
            return toResult(record, true);
        });
    }

    private IssuedKsIdentityResult assertMatchingOwnership(KsIdentityRecord record, String requestHash) {
        if (!record.issuanceRequestHash().equals(requestHash)) {
            meterRegistry.counter(IdentityMetricNames.IDENTITY_ISSUANCE_CONFLICT).increment();
            throw new IssuanceOwnershipConflictException(
                    "Issuance request key already owns an identity with a different request fingerprint");
        }
        return toResult(record, true);
    }

    private IssuedKsIdentityResult toResult(KsIdentityRecord record, boolean replayed) {
        return new IssuedKsIdentityResult(
                record.id(),
                record.canonicalKsNumber(),
                record.sequenceNumber(),
                record.identityType(),
                record.status(),
                replayed);
    }

    private IssuedKsIdentityResult toResult(Map<String, Object> body, boolean replayed) {
        return new IssuedKsIdentityResult(
                UUID.fromString((String) body.get("identity_id")),
                KsNumber.parse((String) body.get("canonical_ks_number")),
                ((Number) body.get("sequence_number")).longValue(),
                IdentityType.valueOf((String) body.get("identity_type")),
                IdentityStatus.valueOf((String) body.get("status")),
                replayed);
    }

    private Map<String, Object> toResponseMap(IssuedKsIdentityResult result) {
        Map<String, Object> body = new HashMap<>();
        body.put("identity_id", result.identityId().toString());
        body.put("canonical_ks_number", result.canonicalKsNumber().canonicalValue());
        body.put("sequence_number", result.sequenceNumber());
        body.put("identity_type", result.identityType().name());
        body.put("status", result.status().name());
        return body;
    }

    private void validate(IssueKsIdentityCommand command) {
        if (command.issuanceRequestKey() == null || command.issuanceRequestKey().isBlank()) {
            throw new IllegalArgumentException("Issuance request key is required");
        }
        if (command.identityType() == null) {
            throw new IllegalArgumentException("Identity type is required");
        }
        if (command.actorContext() == null) {
            throw new IllegalArgumentException("Actor context is required");
        }
        if (command.displayName() != null && command.displayName().length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Display name exceeds maximum length");
        }
    }

    private String toRequestBody(IssueKsIdentityCommand command) {
        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("issuance_request_key", command.issuanceRequestKey());
            payload.put("identity_type", command.identityType().name());
            payload.put("display_name", command.displayName());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize issuance command", ex);
        }
    }
}
