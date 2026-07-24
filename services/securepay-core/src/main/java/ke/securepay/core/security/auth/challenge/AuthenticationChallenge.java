package ke.securepay.core.security.auth.challenge;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthenticationChallenge(
        UUID id,
        UUID identityId,
        String challengeDigest,
        AuthenticationChallengeStatus status,
        String applicationId,
        String deviceId,
        String sourceIpHash,
        Instant expiresAt,
        Instant consumedAt,
        Instant revokedAt,
        Instant createdAt,
        long version) {

    public AuthenticationChallenge {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(identityId, "identityId");
        Objects.requireNonNull(challengeDigest, "challengeDigest");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");

        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
