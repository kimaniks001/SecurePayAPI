package ke.securepay.doctrine;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Phase14MigrationDoctrineTest {

    private static final Path MIGRATION =
            Path.of("database/migrations/V20260723140000__authentication_credential_foundation.sql");

    private static final Set<String> PROHIBITED_TABLE_TOKENS =
            Set.of(
                    "securelink",
                    "journal",
                    "ledger_account",
                    "payment_intent",
                    "otp",
                    "session",
                    "token",
                    "choice_bank");

    @Test
    void phase14MigrationCreatesAuthenticationSchemaObjectsOnly() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create schema if not exists authentication");
        assertThat(sql).contains("authentication.authentication_credentials");
        assertThat(sql).contains("references identity.ks_identities (id)");
        assertThat(sql).contains("password_hash");
        assertThat(sql).contains("active");
        assertThat(sql).contains("version");

        for (String token : PROHIBITED_TABLE_TOKENS) {
            assertThat(sql).doesNotMatch(
                    "create table if not exists [a-z0-9_.]*" + token);
        }

        Pattern createTable =
                Pattern.compile("create table(?: if not exists)?\\s+([a-z0-9_.]+)");

        try (Stream<String> tables = Files.lines(MIGRATION)
                .map(String::toLowerCase)
                .flatMap(line ->
                        createTable.matcher(line).results().map(match -> match.group(1)))) {
            tables.forEach(
                    qualifiedName ->
                            assertThat(qualifiedName).startsWith("authentication."));
        }
    }

    @Test
    void phase14MigrationProtectsCredentialIntegrity() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertThat(sql).contains("identity_id         uuid primary key");
        assertThat(sql).contains("password_hash       varchar(255) not null");
        assertThat(sql).contains("authentication_credentials_password_hash_not_blank");
        assertThat(sql).contains("authentication_credentials_version_non_negative");
        assertThat(sql).contains("authentication_credentials_deactivated_when_inactive");
        assertThat(sql).contains("authentication_credentials_updated_after_created");
    }
}
