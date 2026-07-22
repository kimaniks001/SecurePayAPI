package ke.securepay.platform.identity.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ke.securepay.platform.identity.ksnumber.KsNumber;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.model.KsIdentityRecord;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KsIdentityRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KsIdentityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(KsIdentityRecord record) {
        jdbcTemplate.update(
                """
                INSERT INTO identity.ks_identities (
                    id, canonical_ks_number, sequence_number, identity_type, status, display_name,
                    issuance_request_key, issuance_request_hash, created_by_actor_type, created_by_actor_id,
                    request_id, correlation_id, created_at, updated_at, suspended_at, closed_at, version
                ) VALUES (
                    :id, :canonicalKsNumber, :sequenceNumber, :identityType, :status, :displayName,
                    :issuanceRequestKey, :issuanceRequestHash, :createdByActorType, :createdByActorId,
                    :requestId, :correlationId, :createdAt, :updatedAt, :suspendedAt, :closedAt, :version
                )
                """,
                params(record));
    }

    public Optional<KsIdentityRecord> findById(UUID id) {
        return queryOne("SELECT * FROM identity.ks_identities WHERE id = :id", Map.of("id", id));
    }

    public Optional<KsIdentityRecord> findByCanonicalKsNumber(String canonicalKsNumber) {
        return queryOne(
                "SELECT * FROM identity.ks_identities WHERE canonical_ks_number = :canonicalKsNumber",
                Map.of("canonicalKsNumber", canonicalKsNumber));
    }

    public Optional<KsIdentityRecord> findByIssuanceRequestKey(String issuanceRequestKey) {
        return queryOne(
                "SELECT * FROM identity.ks_identities WHERE issuance_request_key = :issuanceRequestKey",
                Map.of("issuanceRequestKey", issuanceRequestKey));
    }

    public void updateStatus(
            UUID id,
            long expectedVersion,
            IdentityStatus status,
            Instant updatedAt,
            Instant suspendedAt,
            Instant closedAt) {
        int updated = jdbcTemplate.update(
                """
                UPDATE identity.ks_identities
                SET status = :status,
                    updated_at = :updatedAt,
                    suspended_at = :suspendedAt,
                    closed_at = :closedAt,
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("status", status.name())
                        .addValue("updatedAt", Timestamp.from(updatedAt))
                        .addValue("suspendedAt", suspendedAt == null ? null : Timestamp.from(suspendedAt))
                        .addValue("closedAt", closedAt == null ? null : Timestamp.from(closedAt)));
        if (updated == 0) {
            throw new OptimisticLockException("Identity version conflict");
        }
    }

    private Optional<KsIdentityRecord> queryOne(String sql, Map<String, ?> params) {
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapRow(rs)).stream().findFirst();
    }

    private MapSqlParameterSource params(KsIdentityRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("canonicalKsNumber", record.canonicalKsNumber().canonicalValue())
                .addValue("sequenceNumber", record.sequenceNumber())
                .addValue("identityType", record.identityType().name())
                .addValue("status", record.status().name())
                .addValue("displayName", record.displayName())
                .addValue("issuanceRequestKey", record.issuanceRequestKey())
                .addValue("issuanceRequestHash", record.issuanceRequestHash())
                .addValue("createdByActorType", record.createdByActorType())
                .addValue("createdByActorId", record.createdByActorId())
                .addValue("requestId", record.requestId())
                .addValue("correlationId", record.correlationId())
                .addValue("createdAt", Timestamp.from(record.createdAt()))
                .addValue("updatedAt", Timestamp.from(record.updatedAt()))
                .addValue("suspendedAt", record.suspendedAt() == null ? null : Timestamp.from(record.suspendedAt()))
                .addValue("closedAt", record.closedAt() == null ? null : Timestamp.from(record.closedAt()))
                .addValue("version", record.version());
    }

    private KsIdentityRecord mapRow(ResultSet rs) throws SQLException {
        return new KsIdentityRecord(
                rs.getObject("id", UUID.class),
                KsNumber.parse(rs.getString("canonical_ks_number")),
                rs.getLong("sequence_number"),
                IdentityType.valueOf(rs.getString("identity_type")),
                IdentityStatus.valueOf(rs.getString("status")),
                rs.getString("display_name"),
                rs.getString("issuance_request_key"),
                rs.getString("issuance_request_hash"),
                rs.getString("created_by_actor_type"),
                rs.getString("created_by_actor_id"),
                rs.getString("request_id"),
                rs.getString("correlation_id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("suspended_at") == null ? null : rs.getTimestamp("suspended_at").toInstant(),
                rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toInstant(),
                rs.getLong("version"));
    }
}
