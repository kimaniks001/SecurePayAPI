package ke.securepay.core.security.auth;

/**
 * Raised when supplied credentials cannot be verified.
 *
 * The message must not reveal whether the KSNumber or password failed.
 */
public final class InvalidCredentialsException extends AuthenticationException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
