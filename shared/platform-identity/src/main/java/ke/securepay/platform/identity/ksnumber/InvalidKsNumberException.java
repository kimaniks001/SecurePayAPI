package ke.securepay.platform.identity.ksnumber;

/** Raised when a canonical KS Number value cannot be parsed or validated. */
public class InvalidKsNumberException extends IllegalArgumentException {

    public InvalidKsNumberException(String message) {
        super(message);
    }
}
