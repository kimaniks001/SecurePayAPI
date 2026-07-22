package ke.securepay.doctrine;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Phase4MigrationDoctrineTest {

    private static final Path MIGRATION =
            Path.of("database/migrations/V20260723130000__ks_identity_foundation.sql");

    private static final Set<String> PROHIBITED_TABLE_TOKENS =
            Set.of("securelink", "journal", "ledger_account", "payment_intent", "user_account", "otp", "choice_bank");

    @Test
    void phase4MigrationCreatesIdentitySchemaObjectsOnly() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create schema if not exists identity");
        assertThat(sql).contains("identity.ks_identities");
        assertThat(sql).contains("identity.ks_number_aliases");
        assertThat(sql).contains("identity.ks_number_sequence");

        for (String token : PROHIBITED_TABLE_TOKENS) {
            assertThat(sql).doesNotMatch("create table if not exists [a-z0-9_.]*" + token);
        }

        Pattern createTable = Pattern.compile("create table(?: if not exists)?\\s+([a-z0-9_.]+)");
        try (Stream<String> tables = Files.lines(MIGRATION)
                .map(String::toLowerCase)
                .flatMap(line -> createTable.matcher(line).results().map(match -> match.group(1)))) {
            tables.forEach(qualifiedName -> assertThat(qualifiedName).startsWith("identity."));
        }
    }
}
