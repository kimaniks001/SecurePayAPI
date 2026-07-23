package ke.securepay.core.security.auth;

import java.util.Objects;
import ke.securepay.core.security.auth.credential.AuthenticationCredential;
import ke.securepay.core.security.auth.credential.AuthenticationCredentialRepository;
import ke.securepay.core.security.password.PasswordVerifier;
import org.springframework.stereotype.Service;

/**
 * Default username-and-password authentication implementation.
 *
 * Missing accounts, inactive accounts and incorrect passwords deliberately
 * produce the same public failure.
 */
@Service
public final class DefaultAuthenticationService implements AuthenticationService {

    private static final String AUTHENTICATION_METHOD = "PASSWORD";

    private final AuthenticationCredentialRepository credentialRepository;
    private final PasswordVerifier passwordVerifier;

    public DefaultAuthenticationService(
            AuthenticationCredentialRepository credentialRepository,
            PasswordVerifier passwordVerifier) {
        this.credentialRepository =
                Objects.requireNonNull(credentialRepository, "credentialRepository");
        this.passwordVerifier =
                Objects.requireNonNull(passwordVerifier, "passwordVerifier");
    }

    @Override
    public AuthenticationResult authenticate(AuthenticateCommand command) {
        Objects.requireNonNull(command, "command");

        AuthenticationCredential credential = credentialRepository
                .findByKsNumber(command.ksNumber())
                .filter(AuthenticationCredential::active)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordVerifier.matches(command.password(), credential.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        AuthenticationPrincipal principal = new AuthenticationPrincipal(
                credential.actorId(),
                credential.ksNumber(),
                credential.displayName(),
                AUTHENTICATION_METHOD,
                command.applicationId(),
                command.deviceId(),
                command.sourceIpHash());

        return new AuthenticationResult(principal, true);
    }
}
