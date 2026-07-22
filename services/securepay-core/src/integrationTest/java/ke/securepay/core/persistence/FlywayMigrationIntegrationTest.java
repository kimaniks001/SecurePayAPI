package ke.securepay.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@SecurePayIntegrationTest
class FlywayMigrationIntegrationTest {

    private static final Set<String> EXPECTED_SCHEMAS = Set.of("platform", "audit", "events", "idempotency");
    private static final Set<String> EXPECTED_TABLES = Set.of(
            "platform.platform_metadata",
            "platform.technical_test_records",
            "audit.audit_events",
            "events.outbox_events",
            "idempotency.idempotency_records");
    private static final Set<String> PROHIBITED_TABLE_FRAGMENTS = Set.of(
            "ks_number", "securelink", "journal", "ledger_account", "payment_intent", "user_account", "otp");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void flywayMigrationsApplyCleanlyWithExpectedSchemasAndTables() {
        flyway.validate();
        assertThat(flyway.info().pending().length).isZero();

        List<String> schemas = jdbcTemplate.queryForList(
                """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name IN ('platform', 'audit', 'events', 'idempotency')
                """,
                String.class);
        assertThat(schemas).containsExactlyInAnyOrderElementsOf(EXPECTED_SCHEMAS);

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_schema || '.' || table_name
                FROM information_schema.tables
                WHERE table_schema IN ('platform', 'audit', 'events', 'idempotency')
                """,
                String.class);
        assertThat(tables).containsAll(EXPECTED_TABLES);

        Set<String> lowered = tables.stream().map(String::toLowerCase).collect(Collectors.toSet());
        for (String fragment : PROHIBITED_TABLE_FRAGMENTS) {
            assertThat(lowered).noneMatch(name -> name.contains(fragment));
        }
    }

    @Test
    void platformMetadataWasRelocatedFromPublicToPlatformSchema() {
        Integer platformTableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'platform' AND table_name = 'platform_metadata'
                """,
                Integer.class);
        Integer publicTableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'platform_metadata'
                """,
                Integer.class);

        assertThat(platformTableCount)
                .as("platform.platform_metadata must exist after migrations")
                .isEqualTo(1);
        assertThat(publicTableCount)
                .as("public.platform_metadata must not remain after Phase 3 relocation")
                .isZero();
        assertThat(platformTableCount + publicTableCount)
                .as("only one platform_metadata table must exist across schemas")
                .isEqualTo(1);

        String platformPhase = jdbcTemplate.queryForObject(
                """
                SELECT metadata_value
                FROM platform.platform_metadata
                WHERE metadata_key = 'platform_phase'
                """,
                String.class);
        assertThat(platformPhase).isEqualTo("phase-03-database-audit-idempotency-foundation");
    }

    @Test
    void idempotencyScopeUniqueIndexExists() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'idempotency'
                  AND tablename = 'idempotency_records'
                  AND indexname = 'uq_idempotency_scope'
                """,
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void outboxAggregateIndexExists() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'events'
                  AND tablename = 'outbox_events'
                  AND indexname = 'idx_outbox_aggregate'
                """,
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void auditImmutabilityTriggersExist() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_trigger t
                JOIN pg_class c ON c.oid = t.tgrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'audit'
                  AND c.relname = 'audit_events'
                  AND NOT t.tgisinternal
                """,
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}
