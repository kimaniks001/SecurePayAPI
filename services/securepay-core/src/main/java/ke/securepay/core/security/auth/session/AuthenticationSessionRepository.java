package ke.securepay.core.security.auth.session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticationSessionRepository {

    void insert(AuthenticationSession session);

    Optional<AuthenticationSession> findById(UUID sessionId);

    Optional<AuthenticationSession> findByAccessTokenDigest(
            String accessTokenDigest);

    int revokeById(
            UUID sessionId,
            Instant revokedAt,
            AuthenticationSessionStatus status);

    int revokeAllByIdentityId(
            UUID identityId,
            Instant revokedAt,
            AuthenticationSessionStatus status);
}
