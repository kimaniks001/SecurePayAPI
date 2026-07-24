package ke.securepay.core.security.auth.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class AuthenticationCredentialTest {

    @Test
    void retainsCredentialData() {
        AuthenticationCredential credential =
                new AuthenticationCredential(
                        "actor-1",
                        "KS123456",
                        "Wanjiku",
                        "$2a$10$password-hash",
                        true);

        assertThat(credential.actorId()).isEqualTo("actor-1");
        assertThat(credential.ksNumber()).isEqualTo("KS123456");
        assertThat(credential.displayName()).isEqualTo("Wanjiku");
        assertThat(credential.passwordHash())
                .isEqualTo("$2a$10$password-hash");
        assertThat(credential.active()).isTrue();
    }

    @Test
    void requiresAuthenticationFields() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticationCredential(
                        null,
                        "KS123456",
                        "Wanjiku",
                        "$2a$10$password-hash",
                        true))
                .withMessage("actorId");

        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticationCredential(
                        "actor-1",
                        null,
                        "Wanjiku",
                        "$2a$10$password-hash",
                        true))
                .withMessage("ksNumber");

        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticationCredential(
                        "actor-1",
                        "KS123456",
                        "Wanjiku",
                        null,
                        true))
                .withMessage("passwordHash");
    }
}
