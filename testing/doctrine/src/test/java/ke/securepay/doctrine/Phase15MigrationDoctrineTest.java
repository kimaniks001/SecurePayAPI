package ke.securepay.doctrine;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Phase15MigrationDoctrineTest {

    private static final Path MIGRATION =
            Path.of("database/migrations/V20260724150000__authentication_session_lifecycle.sql");

    private static final Set<String> PROHIBITED_TABLE_TOKENS =
            Set.of(
                    "securelink",
                    "journal",
                    "ledger_account",
                    "payment_intent",
                    "choice_bank");

    @Test
    void phase15MigrationCreatesAuthenticationSchemaObjectsOnly() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertThat(sql).contains("create schema if not exists authentication");
        assertThat(sql).contains("authentication.authentication_challenges");
        assertThat(sql).contains("authentication.authentication_sessions");
        assertThat(sql).contains("authentication.refresh_tokens");
        assertThat(sql).contains("references identity.ks_identities (id)");
        assertThat(sql).contains("token_digest");
        assertThat(sql).contains("expires_at");
        assertThat(sql).contains("revoked_at");
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
    void phase15MigrationProtectsSessionLifecycleIntegrity() throws Exception {
        String sql = Files.readString(MIGRATION).toLowerCase(Locale.ROOT);

        assertThat(sql).contains("challenge_digest");
        assertThat(sql).contains("consumed_at");
        assertThat(sql).contains("authentication_challenges_status_check");
        assertThat(sql).contains("authentication_sessions_status_check");
        assertThat(sql).contains("refresh_tokens_status_check");
        assertThat(sql).contains("refresh_tokens_digest_unique");
        assertThat(sql).contains("refresh_tokens_expires_after_created");
        assertThat(sql).contains("authentication_sessions_expires_after_created");
        assertThat(sql).doesNotContain("raw_token");
        assertThat(sql).doesNotContain("plain_token");
        assertThat(sql).doesNotContain("otp_code");
    }
}
