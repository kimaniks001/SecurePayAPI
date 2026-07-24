package ke.securepay.core.security.auth.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticationChallengeTest {

    @Test
    void retainsPendingChallengeData() {
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");
        Instant expiresAt = createdAt.plusSeconds(300);

        AuthenticationChallenge challenge = new AuthenticationChallenge(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "challenge-digest",
                AuthenticationChallengeStatus.PENDING,
                "securepay-web",
                "device-1",
                "ip-hash-1",
                expiresAt,
                null,
                null,
                createdAt,
                0L);

        assertThat(challenge.status()).isEqualTo(AuthenticationChallengeStatus.PENDING);
        assertThat(challenge.challengeDigest()).isEqualTo("challenge-digest");
        assertThat(challenge.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void rejectsNegativeVersion() {
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");

        assertThatThrownBy(() -> new AuthenticationChallenge(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "challenge-digest",
                AuthenticationChallengeStatus.PENDING,
                null,
                null,
                null,
                createdAt.plusSeconds(300),
                null,
                null,
                createdAt,
                -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must not be negative");
    }
}
