package ke.securepay.core.security.auth.challenge.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import ke.securepay.core.security.auth.challenge.AuthenticationChallenge;
import ke.securepay.core.security.auth.challenge.AuthenticationChallengeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcAuthenticationChallengeRepositoryTest {

    @Test
    void insertsChallengeWithNullableRequestContext() {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);

        when(jdbcTemplate.update(anyString(), any(java.util.Map.class)))
                .thenReturn(1);

        Instant createdAt = Instant.parse("2026-07-24T15:00:00Z");

        AuthenticationChallenge challenge =
                new AuthenticationChallenge(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "challenge-digest",
                        AuthenticationChallengeStatus.PENDING,
                        null,
                        null,
                        null,
                        createdAt.plusSeconds(300),
                        null,
                        null,
                        createdAt,
                        0L);

        JdbcAuthenticationChallengeRepository repository =
                new JdbcAuthenticationChallengeRepository(jdbcTemplate);

        assertThatCode(() -> repository.insert(challenge))
                .doesNotThrowAnyException();
    }
}
