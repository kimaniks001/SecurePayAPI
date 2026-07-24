package ke.securepay.core.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class AuthenticationDomainTest {

    @Test
    void authenticationPrincipalRequiresCoreIdentityFields() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticationPrincipal(
                        null,
                        "KS123456",
                        "Wanjiku",
                        "PASSWORD",
                        "securepay-web",
                        "device-1",
                        "ip-hash"))
                .withMessage("actorId");

        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticationPrincipal(
                        "actor-1",
                        null,
                        "Wanjiku",
                        "PASSWORD",
                        "securepay-web",
                        "device-1",
                        "ip-hash"))
                .withMessage("ksNumber");

        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticationPrincipal(
                        "actor-1",
                        "KS123456",
                        "Wanjiku",
                        null,
                        "securepay-web",
                        "device-1",
                        "ip-hash"))
                .withMessage("authenticationMethod");
    }

    @Test
    void authenticateCommandRequiresCredentials() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticateCommand(
                        null,
                        "secret",
                        "securepay-web",
                        "device-1",
                        "ip-hash"))
                .withMessage("ksNumber");

        assertThatNullPointerException()
                .isThrownBy(() -> new AuthenticateCommand(
                        "KS123456",
                        null,
                        "securepay-web",
                        "device-1",
                        "ip-hash"))
                .withMessage("password");
    }

    @Test
    void invalidCredentialsMessageDoesNotRevealFailureReason() {
        InvalidCredentialsException exception =
                new InvalidCredentialsException();

        assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
    }
}
