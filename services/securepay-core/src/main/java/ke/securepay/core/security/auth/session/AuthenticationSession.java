package ke.securepay.core.security.auth.session;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthenticationSession(
        UUID id,
        UUID identityId,
        UUID challengeId,
        String accessTokenDigest,
        AuthenticationSessionStatus status,
        String applicationId,
        String deviceId,
        String sourceIpHash,
        String authenticationMethod,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public AuthenticationSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(identityId, "identityId");
        Objects.requireNonNull(challengeId, "challengeId");
        Objects.requireNonNull(accessTokenDigest, "accessTokenDigest");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(authenticationMethod, "authenticationMethod");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
