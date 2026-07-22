package ke.securepay.platform.common.errors;

/** Foundation error codes only — no business-domain codes in Phase 2. */
public enum FoundationErrorCode {
    VALIDATION_ERROR,
    MALFORMED_JSON,
    UNSUPPORTED_MEDIA_TYPE,
    METHOD_NOT_ALLOWED,
    NOT_FOUND,
    INVALID_CORRELATION_ID,
    INTERNAL_ERROR
}
