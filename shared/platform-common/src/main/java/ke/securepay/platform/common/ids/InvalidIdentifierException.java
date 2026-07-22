package ke.securepay.platform.common.ids;

/** Raised when a request or correlation identifier fails validation. */
public final class InvalidIdentifierException extends RuntimeException {

    private final String fieldName;

    public InvalidIdentifierException(String fieldName) {
        super("Invalid identifier for " + fieldName);
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }
}
