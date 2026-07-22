package ke.securepay.doctrine;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Phase3MigrationDoctrineTest {

    private static final Path MIGRATION =
            Path.of("database/migrations/V20260723090000__phase_03_technical_foundations.sql");

    private static final Set<String> APPROVED_SCHEMAS = Set.of("platform", "audit", "events", "idempotency");
    private static final Set<String> PROHIBITED_TABLE_TOKENS =
            Set.of("ks_number", "securelink", "journal", "ledger_account", "payment_intent", "user_account", "otp", "choice_bank");

    @Test
    void phase3MigrationContainsOnlyApprovedTechnicalSchemas() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create schema if not exists platform");
        assertThat(sql).contains("create schema if not exists audit");
        assertThat(sql).contains("create schema if not exists events");
        assertThat(sql).contains("create schema if not exists idempotency");

        for (String token : PROHIBITED_TABLE_TOKENS) {
            assertThat(sql).doesNotContain("create table " + token);
            assertThat(sql).doesNotMatch("create table if not exists [a-z0-9_.]*" + token);
        }

        Pattern createTable = Pattern.compile("create table(?: if not exists)?\\s+([a-z0-9_.]+)");
        try (Stream<String> tables = Files.lines(MIGRATION)
                .map(String::toLowerCase)
                .flatMap(line -> createTable.matcher(line).results().map(match -> match.group(1)))) {
            tables.forEach(qualifiedName -> {
                String schema = qualifiedName.contains(".") ? qualifiedName.split("\\.", 2)[0] : "public";
                assertThat(APPROVED_SCHEMAS)
                        .as("table %s must be in approved schema", qualifiedName)
                        .contains(schema);
            });
        }
    }
}
