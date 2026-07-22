package ke.securepay.platform.identity.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ke.securepay.platform.identity.model.AliasStatus;
import ke.securepay.platform.identity.model.AliasType;
import ke.securepay.platform.identity.model.KsNumberAliasRecord;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KsNumberAliasRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KsNumberAliasRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(KsNumberAliasRecord record) {
        jdbcTemplate.update(
                """
                INSERT INTO identity.ks_number_aliases (
                    id, identity_id, alias, normalized_alias, alias_type, status, is_primary_display_alias,
                    created_by_actor_type, created_by_actor_id, request_id, correlation_id,
                    created_at, updated_at, released_at, version
                ) VALUES (
                    :id, :identityId, :alias, :normalizedAlias, :aliasType, :status, :primaryDisplayAlias,
                    :createdByActorType, :createdByActorId, :requestId, :correlationId,
                    :createdAt, :updatedAt, :releasedAt, :version
                )
                """,
                params(record));
    }

    public Optional<KsNumberAliasRecord> findById(UUID id) {
        return queryOne("SELECT * FROM identity.ks_number_aliases WHERE id = :id", Map.of("id", id));
    }

    public Optional<KsNumberAliasRecord> findByNormalizedAlias(String normalizedAlias) {
        return queryOne(
                "SELECT * FROM identity.ks_number_aliases WHERE normalized_alias = :normalizedAlias",
                Map.of("normalizedAlias", normalizedAlias));
    }

    public void updateStatus(
            UUID id, long expectedVersion, AliasStatus status, Instant updatedAt, Instant releasedAt) {
        int updated = jdbcTemplate.update(
                """
                UPDATE identity.ks_number_aliases
                SET status = :status,
                    updated_at = :updatedAt,
                    released_at = :releasedAt,
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("status", status.name())
                        .addValue("updatedAt", Timestamp.from(updatedAt))
                        .addValue("releasedAt", releasedAt == null ? null : Timestamp.from(releasedAt)));
        if (updated == 0) {
            throw new OptimisticLockException("Alias version conflict");
        }
    }

    private Optional<KsNumberAliasRecord> queryOne(String sql, Map<String, ?> params) {
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapRow(rs)).stream().findFirst();
    }

    private MapSqlParameterSource params(KsNumberAliasRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("identityId", record.identityId())
                .addValue("alias", record.alias())
                .addValue("normalizedAlias", record.normalizedAlias())
                .addValue("aliasType", record.aliasType().name())
                .addValue("status", record.status().name())
                .addValue("primaryDisplayAlias", record.primaryDisplayAlias())
                .addValue("createdByActorType", record.createdByActorType())
                .addValue("createdByActorId", record.createdByActorId())
                .addValue("requestId", record.requestId())
                .addValue("correlationId", record.correlationId())
                .addValue("createdAt", Timestamp.from(record.createdAt()))
                .addValue("updatedAt", Timestamp.from(record.updatedAt()))
                .addValue("releasedAt", record.releasedAt() == null ? null : Timestamp.from(record.releasedAt()))
                .addValue("version", record.version());
    }

    private KsNumberAliasRecord mapRow(ResultSet rs) throws SQLException {
        return new KsNumberAliasRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("identity_id", UUID.class),
                rs.getString("alias"),
                rs.getString("normalized_alias"),
                AliasType.valueOf(rs.getString("alias_type")),
                AliasStatus.valueOf(rs.getString("status")),
                rs.getBoolean("is_primary_display_alias"),
                rs.getString("created_by_actor_type"),
                rs.getString("created_by_actor_id"),
                rs.getString("request_id"),
                rs.getString("correlation_id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("released_at") == null ? null : rs.getTimestamp("released_at").toInstant(),
                rs.getLong("version"));
    }
}
