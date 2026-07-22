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
    private static final String TECHNICAL_OPERATION = "platform.technical.test";
    private static final Duration DEFAULT_LOCK = Duration.ofMinutes(5);
    private static final Duration DEFAULT_EXPIRY = Duration.ofHours(24);

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
        String normalizedKey = IdempotencyKeyValidator.normalizeOrThrow(idempotencyKey);
        String requestHash = IdempotencyKeyValidator.hashRequest(requestBody);
        Instant now = clock.instant();

        Optional<IdempotencyExecutionResult> replay =
                resolveExisting(actor, normalizedKey, requestHash, now);
        if (replay.isPresent()) {
            throw new IdempotencyConflictException("Idempotency key already completed; use replay path");
        }

        IdempotencyRecord created = new IdempotencyRecord(
                UUID.randomUUID(),
                actor.applicationId(),
                actor.actorId(),
                normalizedKey,
                TECHNICAL_OPERATION,
                requestHash,
                requestContentType,
                "technical_test",
                null,
                IdempotencyStatus.IN_PROGRESS,
                null,
                null,
                null,
                null,
                now.plus(DEFAULT_LOCK),
                now.plus(DEFAULT_EXPIRY),
                now,
                null,
                now,
                0L);

        try {
            repository.insertInProgress(created);
            meterRegistry.counter(PersistenceMetricNames.IDEMPOTENCY_CREATED).increment();
            return created;
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord raced = repository
                    .findByScope(actor.applicationId(), actor.actorId(), TECHNICAL_OPERATION, normalizedKey)
                    .orElseThrow(() -> ex);
            resolveExisting(actor, normalizedKey, requestHash, now);
            throw ex;
        }
    }

    @Transactional
    public IdempotencyRecord completeTechnical(UUID id, long expectedVersion, Map<String, Object> responseBody) {
        payloadValidator.sanitize(responseBody);
        repository.markCompleted(id, expectedVersion, 200, "application/json", responseBody);
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public IdempotencyExecutionResult executeTechnical(
            ActorContext actor,
            String idempotencyKey,
            String requestBody,
            String requestContentType,
            Supplier<Map<String, Object>> action) {
        String normalizedKey = IdempotencyKeyValidator.normalizeOrThrow(idempotencyKey);
        String requestHash = IdempotencyKeyValidator.hashRequest(requestBody);
        Instant now = clock.instant();

        Optional<IdempotencyExecutionResult> replay =
                resolveExisting(actor, normalizedKey, requestHash, now);
        if (replay.isPresent()) {
            return replay.get();
        }

        IdempotencyRecord created = acquireTechnicalInProgress(actor, idempotencyKey, requestBody, requestContentType);
        Map<String, Object> responseBody = action.get();
        IdempotencyRecord completed = completeTechnical(created.id(), created.version(), responseBody);
        return IdempotencyExecutionResult.completed(completed, false);
    }

    public void assertOptimisticVersion(UUID id, long expectedVersion) {
        IdempotencyRecord record = repository.findById(id).orElseThrow();
        if (record.version() != expectedVersion) {
            throw new OptimisticLockException("Idempotency record version conflict");
        }
    }

    private Optional<IdempotencyExecutionResult> resolveExisting(
            ActorContext actor, String normalizedKey, String requestHash, Instant now) {
        IdempotencyRecord existing = repository
                .findByScope(actor.applicationId(), actor.actorId(), TECHNICAL_OPERATION, normalizedKey)
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
