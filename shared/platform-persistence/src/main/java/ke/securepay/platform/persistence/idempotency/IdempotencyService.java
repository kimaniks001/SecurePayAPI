package ke.securepay.platform.persistence.idempotency;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import ke.securepay.platform.observability.metrics.PersistenceMetricNames;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.audit.AuditPayloadValidator;
import ke.securepay.platform.persistence.exception.IdempotencyConflictException;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    public static final String TECHNICAL_OPERATION = "platform.technical.test";
    public static final String IDENTITY_ISSUE_OPERATION = "identity.ks-number.issue";
    public static final Duration DEFAULT_LOCK = Duration.ofMinutes(5);
    private static final Duration DEFAULT_EXPIRY = Duration.ofHours(24);
    /** Provisional replay-storage TTL for identity issuance idempotency records only. */
    public static final Duration IDENTITY_ISSUE_REPLAY_STORAGE_EXPIRY = Duration.ofDays(90);

    private final IdempotencyRepository repository;
    private final AuditPayloadValidator payloadValidator;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public IdempotencyService(
            IdempotencyRepository repository,
            AuditPayloadValidator payloadValidator,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.payloadValidator = payloadValidator;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public IdempotencyRecord acquireTechnicalInProgress(
            ActorContext actor, String idempotencyKey, String requestBody, String requestContentType) {
        return acquireInProgress(
                actor,
                TECHNICAL_OPERATION,
                "technical_test",
                idempotencyKey,
                requestBody,
                requestContentType,
                DEFAULT_LOCK,
                DEFAULT_EXPIRY);
    }

    @Transactional
    public IdempotencyRecord completeTechnical(UUID id, long expectedVersion, Map<String, Object> responseBody) {
        return complete(id, expectedVersion, responseBody);
    }

    @Transactional
    public IdempotencyExecutionResult executeTechnical(
            ActorContext actor,
            String idempotencyKey,
            String requestBody,
            String requestContentType,
            Supplier<Map<String, Object>> action) {
        return execute(
                actor,
                TECHNICAL_OPERATION,
                "technical_test",
                idempotencyKey,
                requestBody,
                requestContentType,
                DEFAULT_LOCK,
                DEFAULT_EXPIRY,
                action);
    }

    @Transactional
    public IdempotencyExecutionResult execute(
            ActorContext actor,
            String operationCode,
            String resourceType,
            String idempotencyKey,
            String requestBody,
            String requestContentType,
            Duration lockDuration,
            Duration expiryDuration,
            Supplier<Map<String, Object>> action) {
        String normalizedKey = IdempotencyKeyValidator.normalizeOrThrow(idempotencyKey);
        String requestHash = IdempotencyKeyValidator.hashRequest(requestBody);
        Instant now = clock.instant();

        Optional<IdempotencyExecutionResult> replay =
                resolveExisting(actor, operationCode, normalizedKey, requestHash, now);
        if (replay.isPresent()) {
            return replay.get();
        }

        IdempotencyRecord created = acquireInProgress(
                actor,
                operationCode,
                resourceType,
                idempotencyKey,
                requestBody,
                requestContentType,
                lockDuration,
                expiryDuration);
        Map<String, Object> responseBody = action.get();
        IdempotencyRecord completed = complete(created.id(), created.version(), responseBody);
        return IdempotencyExecutionResult.completed(completed, false);
    }

    @Transactional
    public IdempotencyRecord acquireInProgress(
            ActorContext actor,
            String operationCode,
            String resourceType,
            String idempotencyKey,
            String requestBody,
            String requestContentType,
            Duration lockDuration,
            Duration expiryDuration) {
        String normalizedKey = IdempotencyKeyValidator.normalizeOrThrow(idempotencyKey);
        String requestHash = IdempotencyKeyValidator.hashRequest(requestBody);
        Instant now = clock.instant();

        Optional<IdempotencyExecutionResult> replay =
                resolveExisting(actor, operationCode, normalizedKey, requestHash, now);
        if (replay.isPresent()) {
            throw new IdempotencyConflictException("Idempotency key already completed; use replay path");
        }

        IdempotencyRecord created = new IdempotencyRecord(
                UUID.randomUUID(),
                actor.applicationId(),
                actor.actorId(),
                normalizedKey,
                operationCode,
                requestHash,
                requestContentType,
                resourceType,
                null,
                IdempotencyStatus.IN_PROGRESS,
                null,
                null,
                null,
                null,
                now.plus(lockDuration),
                now.plus(expiryDuration),
                now,
                null,
                now,
                0L);

        try {
            repository.insertInProgress(created);
            meterRegistry.counter(PersistenceMetricNames.IDEMPOTENCY_CREATED).increment();
            return created;
        } catch (DuplicateKeyException ex) {
            repository
                    .findByScope(actor.applicationId(), actor.actorId(), operationCode, normalizedKey)
                    .orElseThrow(() -> ex);
            resolveExisting(actor, operationCode, normalizedKey, requestHash, now);
            throw ex;
        }
    }

    @Transactional
    public IdempotencyRecord complete(UUID id, long expectedVersion, Map<String, Object> responseBody) {
        payloadValidator.sanitize(responseBody);
        repository.markCompleted(id, expectedVersion, 200, "application/json", responseBody);
        return repository.findById(id).orElseThrow();
    }

    public void assertOptimisticVersion(UUID id, long expectedVersion) {
        IdempotencyRecord record = repository.findById(id).orElseThrow();
        if (record.version() != expectedVersion) {
            throw new OptimisticLockException("Idempotency record version conflict");
        }
    }

    private Optional<IdempotencyExecutionResult> resolveExisting(
            ActorContext actor, String operationCode, String normalizedKey, String requestHash, Instant now) {
        IdempotencyRecord existing = repository
                .findByScope(actor.applicationId(), actor.actorId(), operationCode, normalizedKey)
                .orElse(null);
        if (existing == null) {
            return Optional.empty();
        }

        if (!existing.requestHash().equals(requestHash)) {
            meterRegistry.counter(PersistenceMetricNames.IDEMPOTENCY_CONFLICTS).increment();
            log.warn("idempotency_conflict operation={} key={}", existing.operationCode(), existing.idempotencyKey());
            throw new IdempotencyConflictException("Idempotency key reused with different request hash");
        }

        if (existing.expiresAt().isBefore(now)) {
            throw new IdempotencyConflictException("Idempotency record expired");
        }

        if (existing.processingStatus() == IdempotencyStatus.COMPLETED) {
            meterRegistry.counter(PersistenceMetricNames.IDEMPOTENCY_REPLAYED).increment();
            return Optional.of(IdempotencyExecutionResult.completed(existing, true));
        }

        if (existing.processingStatus() == IdempotencyStatus.IN_PROGRESS) {
            if (existing.lockedUntil() != null && existing.lockedUntil().isAfter(now)) {
                meterRegistry.counter(PersistenceMetricNames.IDEMPOTENCY_CONFLICTS).increment();
                throw new IdempotencyConflictException("Idempotency request already in progress");
            }
            repository.refreshLock(existing.id(), existing.version(), now.plus(DEFAULT_LOCK));
            throw new IdempotencyConflictException("Stale in-progress idempotency record requires retry");
        }

        throw new IdempotencyConflictException("Idempotency record is not replayable: " + existing.processingStatus());
    }

    public record IdempotencyExecutionResult(IdempotencyRecord record, boolean replayed) {
        static IdempotencyExecutionResult completed(IdempotencyRecord record, boolean replayed) {
            return new IdempotencyExecutionResult(record, replayed);
        }
    }
}
