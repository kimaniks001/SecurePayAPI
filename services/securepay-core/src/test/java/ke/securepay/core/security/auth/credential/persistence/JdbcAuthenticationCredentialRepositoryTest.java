package ke.securepay.core.security.auth.credential.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import ke.securepay.core.security.auth.credential.AuthenticationCredential;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcAuthenticationCredentialRepositoryTest {

    @Test
    void mapsCredentialByKsNumber() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.getString("actor_id")).thenReturn("actor-1");
        when(resultSet.getString("canonical_ks_number")).thenReturn("KS000001");
        when(resultSet.getString("display_name")).thenReturn("James");
        when(resultSet.getString("password_hash")).thenReturn("stored-hash");
        when(resultSet.getBoolean("active")).thenReturn(true);

        when(jdbcTemplate.query(
                        anyString(),
                        any(java.util.Map.class),
                        any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<AuthenticationCredential> rowMapper =
                            invocation.getArgument(2);
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });

        JdbcAuthenticationCredentialRepository repository =
                new JdbcAuthenticationCredentialRepository(jdbcTemplate);

        Optional<AuthenticationCredential> result =
                repository.findByKsNumber("KS000001");

        assertThat(result)
                .contains(new AuthenticationCredential(
                        "actor-1",
                        "KS000001",
                        "James",
                        "stored-hash",
                        true));
    }

    @Test
    void returnsEmptyWhenCredentialDoesNotExist() {
        NamedParameterJdbcTemplate jdbcTemplate =
                mock(NamedParameterJdbcTemplate.class);

        when(jdbcTemplate.query(
                        anyString(),
                        any(java.util.Map.class),
                        any(RowMapper.class)))
                .thenReturn(List.of());

        JdbcAuthenticationCredentialRepository repository =
                new JdbcAuthenticationCredentialRepository(jdbcTemplate);

        assertThat(repository.findByKsNumber("KS999999")).isEmpty();
    }
}
