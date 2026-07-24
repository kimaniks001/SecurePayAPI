package ke.securepay.core.security.auth;

/**
 * Verifies primary credentials and creates a pending MFA challenge.
 *
 * This step never creates an active authenticated session.
 */
public interface AuthenticationService {

    PendingAuthenticationResult authenticate(AuthenticateCommand command);
}
