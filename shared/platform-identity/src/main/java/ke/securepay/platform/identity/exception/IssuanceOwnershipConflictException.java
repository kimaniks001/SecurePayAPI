package ke.securepay.platform.identity.exception;

/** Raised when an issuance request key is reused with a different request fingerprint. */
public class IssuanceOwnershipConflictException extends IllegalArgumentException {

    public IssuanceOwnershipConflictException(String message) {
        super(message);
    }
}
