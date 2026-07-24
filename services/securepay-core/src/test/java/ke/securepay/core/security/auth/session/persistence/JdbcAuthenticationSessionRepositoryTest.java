package ke.securepay.core.security.auth.session.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ke.securepay.core.security.auth.session.AuthenticationSession;
import ke.securepay.core.security.auth.session.AuthenticationSessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcAuthenticationSessionRepositoryTest {

    @Test
    void insertsSessionWithNullableRequestContext() {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);

        when(jdbcTemplate.update(anyString(), any(java.util.Map.class)))
                .thenReturn(1);

        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");

        AuthenticationSession session =
                new AuthenticationSession(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "access-digest",
                        AuthenticationSessionStatus.ACTIVE,
                        null,
                        null,
                        null,
                        "PASSWORD_OTP",
                        createdAt.plusSeconds(900),
                        null,
                        createdAt,
                        createdAt,
                        0L);

        JdbcAuthenticationSessionRepository repository =
                new JdbcAuthenticationSessionRepository(jdbcTemplate);

        assertThatCode(() -> repository.insert(session))
                .doesNotThrowAnyException();
    }

    @Test
    void mapsSessionById() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);

        UUID sessionId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");
        Instant expiresAt = createdAt.plusSeconds(900);

        when(resultSet.getObject("id", UUID.class)).thenReturn(sessionId);
        when(resultSet.getObject("identity_id", UUID.class))
                .thenReturn(identityId);
        when(resultSet.getObject("challenge_id", UUID.class))
                .thenReturn(challengeId);
        when(resultSet.getString("access_token_digest"))
                .thenReturn("access-digest");
        when(resultSet.getString("status")).thenReturn("ACTIVE");
        when(resultSet.getString("authentication_method"))
                .thenReturn("PASSWORD_OTP");
        when(resultSet.getTimestamp("expires_at"))
                .thenReturn(Timestamp.from(expiresAt));
        when(resultSet.getTimestamp("created_at"))
                .thenReturn(Timestamp.from(createdAt));
        when(resultSet.getTimestamp("updated_at"))
                .thenReturn(Timestamp.from(createdAt));
        when(resultSet.getLong("version")).thenReturn(0L);

        when(jdbcTemplate.query(
                        anyString(),
                        any(java.util.Map.class),
                        any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<AuthenticationSession> rowMapper =
                            invocation.getArgument(2);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });

        JdbcAuthenticationSessionRepository repository =
                new JdbcAuthenticationSessionRepository(jdbcTemplate);

        Optional<AuthenticationSession> result =
                repository.findById(sessionId);

        assertThat(result).contains(
                new AuthenticationSession(
                        sessionId,
                        identityId,
                        challengeId,
                        "access-digest",
                        AuthenticationSessionStatus.ACTIVE,
                        null,
                        null,
                        null,
                        "PASSWORD_OTP",
                        expiresAt,
                        null,
                        createdAt,
                        createdAt,
                        0L));
    }
}
