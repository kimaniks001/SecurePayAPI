package ke.securepay.core.security.auth;

/**
 * Verifies credentials and returns the next authentication state.
 *
 * Session or token creation is intentionally outside this contract.
 */
public interface AuthenticationService {

    AuthenticationResult authenticate(AuthenticateCommand command);
}
