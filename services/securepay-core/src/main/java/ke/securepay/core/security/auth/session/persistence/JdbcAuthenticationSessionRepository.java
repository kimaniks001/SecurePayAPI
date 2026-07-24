package ke.securepay.core.security.auth.session.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ke.securepay.core.security.auth.session.AuthenticationSession;
import ke.securepay.core.security.auth.session.AuthenticationSessionRepository;
import ke.securepay.core.security.auth.session.AuthenticationSessionStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthenticationSessionRepository
        implements AuthenticationSessionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAuthenticationSessionRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(AuthenticationSession session) {
        jdbcTemplate.update(
                """
                INSERT INTO authentication.authentication_sessions (
                    id,
                    identity_id,
                    challenge_id,
                    access_token_digest,
                    status,
                    application_id,
                    device_id,
                    source_ip_hash,
                    authentication_method,
                    expires_at,
                    revoked_at,
                    created_at,
                    updated_at,
                    version
                ) VALUES (
                    :id,
                    :identityId,
                    :challengeId,
                    :accessTokenDigest,
                    :status,
                    :applicationId,
                    :deviceId,
                    :sourceIpHash,
                    :authenticationMethod,
                    :expiresAt,
                    :revokedAt,
                    :createdAt,
                    :updatedAt,
                    :version
                )
                """,
                insertParameters(session));
    }

    @Override
    public Optional<AuthenticationSession> findById(UUID sessionId) {
        return findOne(
                """
                SELECT *
                FROM authentication.authentication_sessions
                WHERE id = :value
                """,
                sessionId);
    }

    @Override
    public Optional<AuthenticationSession> findByAccessTokenDigest(
            String accessTokenDigest) {
        return findOne(
                """
                SELECT *
                FROM authentication.authentication_sessions
                WHERE access_token_digest = :value
                """,
                accessTokenDigest);
    }

    @Override
    public int revokeById(
            UUID sessionId,
            Instant revokedAt,
            AuthenticationSessionStatus status) {
        return jdbcTemplate.update(
                """
                UPDATE authentication.authentication_sessions
                SET status = :status,
                    revoked_at = :revokedAt,
                    updated_at = :revokedAt,
                    version = version + 1
                WHERE id = :sessionId
                  AND status = 'ACTIVE'
                """,
                Map.of(
                        "sessionId", sessionId,
                        "revokedAt", Timestamp.from(revokedAt),
                        "status", status.name()));
    }

    @Override
    public int revokeAllByIdentityId(
            UUID identityId,
            Instant revokedAt,
            AuthenticationSessionStatus status) {
        return jdbcTemplate.update(
                """
                UPDATE authentication.authentication_sessions
                SET status = :status,
                    revoked_at = :revokedAt,
                    updated_at = :revokedAt,
                    version = version + 1
                WHERE identity_id = :identityId
                  AND status = 'ACTIVE'
                """,
                Map.of(
                        "identityId", identityId,
                        "revokedAt", Timestamp.from(revokedAt),
                        "status", status.name()));
    }

    private Optional<AuthenticationSession> findOne(
            String sql,
            Object value) {
        return jdbcTemplate
                .query(
                        sql,
                        Map.of("value", value),
                        (resultSet, rowNumber) -> new AuthenticationSession(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getObject("identity_id", UUID.class),
                                resultSet.getObject("challenge_id", UUID.class),
                                resultSet.getString("access_token_digest"),
                                AuthenticationSessionStatus.valueOf(
                                        resultSet.getString("status")),
                                resultSet.getString("application_id"),
                                resultSet.getString("device_id"),
                                resultSet.getString("source_ip_hash"),
                                resultSet.getString("authentication_method"),
                                resultSet.getTimestamp("expires_at").toInstant(),
                                resultSet.getTimestamp("revoked_at") == null
                                        ? null
                                        : resultSet.getTimestamp(
                                                "revoked_at").toInstant(),
                                resultSet.getTimestamp("created_at").toInstant(),
                                resultSet.getTimestamp("updated_at").toInstant(),
                                resultSet.getLong("version")))
                .stream()
                .findFirst();
    }

    private Map<String, Object> insertParameters(
            AuthenticationSession session) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", session.id());
        parameters.put("identityId", session.identityId());
        parameters.put("challengeId", session.challengeId());
        parameters.put("accessTokenDigest", session.accessTokenDigest());
        parameters.put("status", session.status().name());
        parameters.put("applicationId", session.applicationId());
        parameters.put("deviceId", session.deviceId());
        parameters.put("sourceIpHash", session.sourceIpHash());
        parameters.put(
                "authenticationMethod",
                session.authenticationMethod());
        parameters.put("expiresAt", Timestamp.from(session.expiresAt()));
        parameters.put("revokedAt", session.revokedAt());
        parameters.put("createdAt", Timestamp.from(session.createdAt()));
        parameters.put("updatedAt", Timestamp.from(session.updatedAt()));
        parameters.put("version", session.version());
        return parameters;
    }
}
