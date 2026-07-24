package ke.securepay.core.security.auth.token;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID sessionId,
        String tokenDigest,
        RefreshTokenStatus status,
        UUID parentTokenId,
        UUID replacedByTokenId,
        Instant expiresAt,
        Instant rotatedAt,
        Instant revokedAt,
        Instant createdAt,
        long version) {

    public RefreshToken {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(tokenDigest, "tokenDigest");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");

        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
