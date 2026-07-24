package ke.securepay.core.security.auth.token;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void insert(RefreshToken token);

    Optional<RefreshToken> findByTokenDigest(String tokenDigest);

    boolean rotate(
            RefreshToken currentToken,
            RefreshToken replacementToken,
            Instant rotatedAt);

    int revokeAllBySessionId(
            UUID sessionId,
            Instant revokedAt,
            RefreshTokenStatus status);
}
