package ke.securepay.core.security.auth.credential.persistence;

import java.util.Map;
import java.util.Optional;
import ke.securepay.core.security.auth.credential.AuthenticationCredential;
import ke.securepay.core.security.auth.credential.AuthenticationCredentialRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthenticationCredentialRepository
        implements AuthenticationCredentialRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAuthenticationCredentialRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AuthenticationCredential> findByKsNumber(String ksNumber) {
        return jdbcTemplate
                .query(
                        """
                        SELECT
                            identity.id::text AS actor_id,
                            identity.canonical_ks_number,
                            identity.display_name,
                            credential.password_hash,
                            credential.active
                        FROM authentication.authentication_credentials credential
                        JOIN identity.ks_identities identity
                          ON identity.id = credential.identity_id
                        WHERE identity.canonical_ks_number = :ksNumber
                        """,
                        Map.of("ksNumber", ksNumber),
                        (resultSet, rowNumber) ->
                                new AuthenticationCredential(
                                        resultSet.getString("actor_id"),
                                        resultSet.getString("canonical_ks_number"),
                                        resultSet.getString("display_name"),
                                        resultSet.getString("password_hash"),
                                        resultSet.getBoolean("active")))
                .stream()
                .findFirst();
    }
}
