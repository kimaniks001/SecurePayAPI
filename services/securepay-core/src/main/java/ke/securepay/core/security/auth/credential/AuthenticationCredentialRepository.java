package ke.securepay.core.security.auth.credential;

import java.util.Optional;

/**
 * Retrieves authentication credentials without exposing persistence details.
 */
public interface AuthenticationCredentialRepository {

    Optional<AuthenticationCredential> findByKsNumber(String ksNumber);
}
