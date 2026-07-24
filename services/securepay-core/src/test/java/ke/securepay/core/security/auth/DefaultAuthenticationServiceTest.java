package ke.securepay.core.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import ke.securepay.core.security.auth.credential.AuthenticationCredential;
import ke.securepay.core.security.auth.credential.AuthenticationCredentialRepository;
import ke.securepay.core.security.password.PasswordVerifier;
import org.junit.jupiter.api.Test;

class DefaultAuthenticationServiceTest {

    private static final AuthenticateCommand COMMAND = new AuthenticateCommand(
            "KS000001",
            "correct-password",
            "securepay-web",
            "device-1",
            "ip-hash-1");

    @Test
    void authenticatesActiveCredentialWithMatchingPassword() {
        AuthenticationCredential credential = new AuthenticationCredential(
                "actor-1",
                "KS000001",
                "James",
                "stored-hash",
                true);

        AuthenticationCredentialRepository repository =
                ksNumber -> Optional.of(credential);

        PasswordVerifier verifier =
                (rawPassword, passwordHash) ->
                        rawPassword.equals("correct-password")
                                && passwordHash.equals("stored-hash");

        DefaultAuthenticationService service =
                new DefaultAuthenticationService(repository, verifier);

        AuthenticationResult result = service.authenticate(COMMAND);

        assertThat(result.otpRequired()).isTrue();
        assertThat(result.principal()).isEqualTo(new AuthenticationPrincipal(
                "actor-1",
                "KS000001",
                "James",
                "PASSWORD",
                "securepay-web",
                "device-1",
                "ip-hash-1"));
    }

    @Test
    void rejectsUnknownKsNumber() {
        AuthenticationCredentialRepository repository =
                ksNumber -> Optional.empty();

        DefaultAuthenticationService service =
                new DefaultAuthenticationService(repository, (raw, hash) -> true);

        assertThatThrownBy(() -> service.authenticate(COMMAND))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void rejectsInactiveCredential() {
        AuthenticationCredential credential = new AuthenticationCredential(
                "actor-1",
                "KS000001",
                "James",
                "stored-hash",
                false);

        DefaultAuthenticationService service =
                new DefaultAuthenticationService(
                        ksNumber -> Optional.of(credential),
                        (raw, hash) -> true);

        assertThatThrownBy(() -> service.authenticate(COMMAND))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void rejectsIncorrectPassword() {
        AuthenticationCredential credential = new AuthenticationCredential(
                "actor-1",
                "KS000001",
                "James",
                "stored-hash",
                true);

        DefaultAuthenticationService service =
                new DefaultAuthenticationService(
                        ksNumber -> Optional.of(credential),
                        (raw, hash) -> false);

        assertThatThrownBy(() -> service.authenticate(COMMAND))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }
}
