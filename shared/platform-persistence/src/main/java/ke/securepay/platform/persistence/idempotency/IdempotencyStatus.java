package ke.securepay.platform.persistence.idempotency;

public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    EXPIRED
}
