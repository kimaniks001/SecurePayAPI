package ke.securepay.core.security.auth.token.persistence;

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
import ke.securepay.core.security.auth.token.RefreshToken;
import ke.securepay.core.security.auth.token.RefreshTokenStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcRefreshTokenRepositoryTest {

    @Test
    void insertsTokenWithNullableLifecycleFields() {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);

        when(jdbcTemplate.update(anyString(), any(java.util.Map.class)))
                .thenReturn(1);

        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");

        RefreshToken token =
                new RefreshToken(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "refresh-digest",
                        RefreshTokenStatus.ACTIVE,
                        null,
                        null,
                        createdAt.plusSeconds(3600),
                        null,
                        null,
                        createdAt,
                        0L);

        JdbcRefreshTokenRepository repository =
                new JdbcRefreshTokenRepository(jdbcTemplate);

        assertThatCode(() -> repository.insert(token))
                .doesNotThrowAnyException();
    }

    @Test
    void mapsTokenByDigest() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);

        UUID tokenId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");
        Instant expiresAt = createdAt.plusSeconds(3600);

        when(resultSet.getObject("id", UUID.class)).thenReturn(tokenId);
        when(resultSet.getObject("session_id", UUID.class))
                .thenReturn(sessionId);
        when(resultSet.getString("token_digest"))
                .thenReturn("refresh-digest");
        when(resultSet.getString("status")).thenReturn("ACTIVE");
        when(resultSet.getTimestamp("expires_at"))
                .thenReturn(Timestamp.from(expiresAt));
        when(resultSet.getTimestamp("created_at"))
                .thenReturn(Timestamp.from(createdAt));
        when(resultSet.getLong("version")).thenReturn(0L);

        when(jdbcTemplate.query(
                        anyString(),
                        any(java.util.Map.class),
                        any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<RefreshToken> rowMapper =
                            invocation.getArgument(2);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });

        JdbcRefreshTokenRepository repository =
                new JdbcRefreshTokenRepository(jdbcTemplate);

        Optional<RefreshToken> result =
                repository.findByTokenDigest("refresh-digest");

        assertThat(result).contains(
                new RefreshToken(
                        tokenId,
                        sessionId,
                        "refresh-digest",
                        RefreshTokenStatus.ACTIVE,
                        null,
                        null,
                        expiresAt,
                        null,
                        null,
                        createdAt,
                        0L));
    }

    @Test
    void insertsReplacementOnlyAfterCurrentTokenRotates() {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);

        when(jdbcTemplate.update(anyString(), any(java.util.Map.class)))
                .thenReturn(1);

        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");
        UUID sessionId = UUID.randomUUID();

        RefreshToken current =
                new RefreshToken(
                        UUID.randomUUID(),
                        sessionId,
                        "current-digest",
                        RefreshTokenStatus.ACTIVE,
                        null,
                        null,
                        createdAt.plusSeconds(3600),
                        null,
                        null,
                        createdAt,
                        0L);

        RefreshToken replacement =
                new RefreshToken(
                        UUID.randomUUID(),
                        sessionId,
                        "replacement-digest",
                        RefreshTokenStatus.ACTIVE,
                        current.id(),
                        null,
                        createdAt.plusSeconds(7200),
                        null,
                        null,
                        createdAt.plusSeconds(10),
                        0L);

        JdbcRefreshTokenRepository repository =
                new JdbcRefreshTokenRepository(jdbcTemplate);

        assertThat(repository.rotate(
                current,
                replacement,
                createdAt.plusSeconds(10))).isTrue();
    }
}
