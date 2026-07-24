package ke.securepay.core.security.auth.challenge.persistence;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ke.securepay.core.security.auth.challenge.AuthenticationChallenge;
import ke.securepay.core.security.auth.challenge.AuthenticationChallengeRepository;
import ke.securepay.core.security.auth.challenge.AuthenticationChallengeStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthenticationChallengeRepository
        implements AuthenticationChallengeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAuthenticationChallengeRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(AuthenticationChallenge challenge) {
        jdbcTemplate.update(
                """
                INSERT INTO authentication.authentication_challenges (
                    id,
                    identity_id,
                    challenge_digest,
                    status,
                    application_id,
                    device_id,
                    source_ip_hash,
                    expires_at,
                    consumed_at,
                    revoked_at,
                    created_at,
                    version
                ) VALUES (
                    :id,
                    :identityId,
                    :challengeDigest,
                    :status,
                    :applicationId,
                    :deviceId,
                    :sourceIpHash,
                    :expiresAt,
                    :consumedAt,
                    :revokedAt,
                    :createdAt,
                    :version
                )
                """,
                insertParameters(challenge));
    }

    @Override
    public Optional<AuthenticationChallenge> findByChallengeDigest(
            String challengeDigest) {
        return jdbcTemplate
                .query(
                        """
                        SELECT
                            id,
                            identity_id,
                            challenge_digest,
                            status,
                            application_id,
                            device_id,
                            source_ip_hash,
                            expires_at,
                            consumed_at,
                            revoked_at,
                            created_at,
                            version
                        FROM authentication.authentication_challenges
                        WHERE challenge_digest = :challengeDigest
                        """,
                        Map.of("challengeDigest", challengeDigest),
                        (resultSet, rowNumber) -> new AuthenticationChallenge(
                                resultSet.getObject("id", java.util.UUID.class),
                                resultSet.getObject(
                                        "identity_id",
                                        java.util.UUID.class),
                                resultSet.getString("challenge_digest"),
                                AuthenticationChallengeStatus.valueOf(
                                        resultSet.getString("status")),
                                resultSet.getString("application_id"),
                                resultSet.getString("device_id"),
                                resultSet.getString("source_ip_hash"),
                                resultSet.getTimestamp("expires_at").toInstant(),
                                resultSet.getTimestamp("consumed_at") == null
                                        ? null
                                        : resultSet.getTimestamp(
                                                "consumed_at").toInstant(),
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
    public boolean consumePendingChallenge(
            AuthenticationChallenge challenge) {
        return jdbcTemplate.update(
                        """
                        UPDATE authentication.authentication_challenges
                        SET status = 'CONSUMED',
                            consumed_at = :consumedAt,
                            version = version + 1
                        WHERE id = :id
                          AND status = 'PENDING'
                          AND version = :version
                        """,
                        Map.of(
                                "id", challenge.id(),
                                "consumedAt", Timestamp.from(
                                        challenge.consumedAt()),
                                "version", challenge.version()))
                == 1;
    }

    private Map<String, Object> insertParameters(
            AuthenticationChallenge challenge) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", challenge.id());
        parameters.put("identityId", challenge.identityId());
        parameters.put("challengeDigest", challenge.challengeDigest());
        parameters.put("status", challenge.status().name());
        parameters.put("applicationId", challenge.applicationId());
        parameters.put("deviceId", challenge.deviceId());
        parameters.put("sourceIpHash", challenge.sourceIpHash());
        parameters.put("expiresAt", Timestamp.from(challenge.expiresAt()));
        parameters.put("consumedAt", challenge.consumedAt());
        parameters.put("revokedAt", challenge.revokedAt());
        parameters.put("createdAt", Timestamp.from(challenge.createdAt()));
        parameters.put("version", challenge.version());
        return parameters;
    }
}
