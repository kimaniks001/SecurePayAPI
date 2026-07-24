package ke.securepay.core.security.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticationSessionTest {

    @Test
    void retainsActiveSessionData() {
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");
        Instant expiresAt = createdAt.plusSeconds(900);

        AuthenticationSession session = new AuthenticationSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "access-token-digest",
                AuthenticationSessionStatus.ACTIVE,
                "securepay-web",
                "device-1",
                "ip-hash-1",
                "PASSWORD_OTP",
                expiresAt,
                null,
                createdAt,
                createdAt,
                0L);

        assertThat(session.status())
                .isEqualTo(AuthenticationSessionStatus.ACTIVE);
        assertThat(session.accessTokenDigest())
                .isEqualTo("access-token-digest");
        assertThat(session.authenticationMethod())
                .isEqualTo("PASSWORD_OTP");
    }

    @Test
    void rejectsNegativeVersion() {
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");

        assertThatThrownBy(() -> new AuthenticationSession(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "access-token-digest",
                AuthenticationSessionStatus.ACTIVE,
                null,
                null,
                null,
                "PASSWORD_OTP",
                createdAt.plusSeconds(900),
                null,
                createdAt,
                createdAt,
                -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must not be negative");
    }
}
