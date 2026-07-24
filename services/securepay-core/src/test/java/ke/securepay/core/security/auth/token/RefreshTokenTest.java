package ke.securepay.core.security.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    void retainsActiveRefreshTokenData() {
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");
        Instant expiresAt = createdAt.plusSeconds(86_400);

        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "refresh-token-digest",
                RefreshTokenStatus.ACTIVE,
                null,
                null,
                expiresAt,
                null,
                null,
                createdAt,
                0L);

        assertThat(token.status()).isEqualTo(RefreshTokenStatus.ACTIVE);
        assertThat(token.tokenDigest()).isEqualTo("refresh-token-digest");
        assertThat(token.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void rejectsNegativeVersion() {
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");

        assertThatThrownBy(() -> new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "refresh-token-digest",
                RefreshTokenStatus.ACTIVE,
                null,
                null,
                createdAt.plusSeconds(86_400),
                null,
                null,
                createdAt,
                -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must not be negative");
    }
}
