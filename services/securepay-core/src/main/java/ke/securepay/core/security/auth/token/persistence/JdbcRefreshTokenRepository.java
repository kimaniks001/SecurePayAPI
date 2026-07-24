package ke.securepay.core.security.auth.token.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ke.securepay.core.security.auth.token.RefreshToken;
import ke.securepay.core.security.auth.token.RefreshTokenRepository;
import ke.securepay.core.security.auth.token.RefreshTokenStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcRefreshTokenRepository
        implements RefreshTokenRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcRefreshTokenRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(RefreshToken token) {
        jdbcTemplate.update(
                """
                INSERT INTO authentication.refresh_tokens (
                    id,
                    session_id,
                    token_digest,
                    status,
                    parent_token_id,
                    replaced_by_token_id,
                    expires_at,
                    rotated_at,
                    revoked_at,
                    created_at,
                    version
                ) VALUES (
                    :id,
                    :sessionId,
                    :tokenDigest,
                    :status,
                    :parentTokenId,
                    :replacedByTokenId,
                    :expiresAt,
                    :rotatedAt,
                    :revokedAt,
                    :createdAt,
                    :version
                )
                """,
                insertParameters(token));
    }

    @Override
    public Optional<RefreshToken> findByTokenDigest(String tokenDigest) {
        return jdbcTemplate
                .query(
                        """
                        SELECT
                            id,
                            session_id,
                            token_digest,
                            status,
                            parent_token_id,
                            replaced_by_token_id,
                            expires_at,
                            rotated_at,
                            revoked_at,
                            created_at,
                            version
                        FROM authentication.refresh_tokens
                        WHERE token_digest = :tokenDigest
                        """,
                        Map.of("tokenDigest", tokenDigest),
                        (resultSet, rowNumber) -> new RefreshToken(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getObject("session_id", UUID.class),
                                resultSet.getString("token_digest"),
                                RefreshTokenStatus.valueOf(
                                        resultSet.getString("status")),
                                resultSet.getObject(
                                        "parent_token_id",
                                        UUID.class),
                                resultSet.getObject(
                                        "replaced_by_token_id",
                                        UUID.class),
                                resultSet.getTimestamp("expires_at").toInstant(),
                                resultSet.getTimestamp("rotated_at") == null
                                        ? null
                                        : resultSet.getTimestamp(
                                                "rotated_at").toInstant(),
                                resultSet.getTimestamp("revoked_at") == null
                                        ? null
                                        : resultSet.getTimestamp(
                                                "revoked_at").toInstant(),
                                resultSet.getTimestamp("created_at").toInstant(),
                                resultSet.getLong("version")))
                .stream()
                .findFirst();
    }

    @Override
    @Transactional
    public boolean rotate(
            RefreshToken currentToken,
            RefreshToken replacementToken,
            Instant rotatedAt) {
        int updated = jdbcTemplate.update(
                """
                UPDATE authentication.refresh_tokens
                SET status = 'ROTATED',
                    replaced_by_token_id = :replacementTokenId,
                    rotated_at = :rotatedAt,
                    version = version + 1
                WHERE id = :currentTokenId
                  AND status = 'ACTIVE'
                  AND version = :version
                """,
                Map.of(
                        "currentTokenId", currentToken.id(),
                        "replacementTokenId", replacementToken.id(),
                        "rotatedAt", Timestamp.from(rotatedAt),
                        "version", currentToken.version()));

        if (updated != 1) {
            return false;
        }

        insert(replacementToken);
        return true;
    }

    @Override
    public int revokeAllBySessionId(
            UUID sessionId,
            Instant revokedAt,
            RefreshTokenStatus status) {
        return jdbcTemplate.update(
                """
                UPDATE authentication.refresh_tokens
                SET status = :status,
                    revoked_at = :revokedAt,
                    version = version + 1
                WHERE session_id = :sessionId
                  AND status = 'ACTIVE'
                """,
                Map.of(
                        "sessionId", sessionId,
                        "revokedAt", Timestamp.from(revokedAt),
                        "status", status.name()));
    }

    private Map<String, Object> insertParameters(RefreshToken token) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", token.id());
        parameters.put("sessionId", token.sessionId());
        parameters.put("tokenDigest", token.tokenDigest());
        parameters.put("status", token.status().name());
        parameters.put("parentTokenId", token.parentTokenId());
        parameters.put("replacedByTokenId", token.replacedByTokenId());
        parameters.put("expiresAt", Timestamp.from(token.expiresAt()));
        parameters.put("rotatedAt", token.rotatedAt());
        parameters.put("revokedAt", token.revokedAt());
        parameters.put("createdAt", Timestamp.from(token.createdAt()));
        parameters.put("version", token.version());
        return parameters;
    }
}
