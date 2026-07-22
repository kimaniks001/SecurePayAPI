package ke.securepay.platform.identity.alias;

/** Raised when an alias cannot be normalized or accepted. */
public class InvalidAliasException extends IllegalArgumentException {

    public InvalidAliasException(String message) {
        super(message);
    }
}
