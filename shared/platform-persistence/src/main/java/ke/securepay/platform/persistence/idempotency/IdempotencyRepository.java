package ke.securepay.platform.persistence.idempotency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public IdempotencyRecord insertInProgress(IdempotencyRecord record) {
        String sql =
                """
                INSERT INTO idempotency.idempotency_records (
                    id, application_id, actor_id, idempotency_key, operation_code, request_hash,
                    request_content_type, resource_type, resource_id, processing_status,
                    locked_until, expires_at, version
                ) VALUES (
                    :id, :applicationId, :actorId, :idempotencyKey, :operationCode, :requestHash,
                    :requestContentType, :resourceType, :resourceId, :processingStatus,
                    :lockedUntil, :expiresAt, :version
                )
                """;
        MapSqlParameterSource params = baseParams(record)
                .addValue("processingStatus", record.processingStatus().name())
                .addValue("lockedUntil", record.lockedUntil() == null ? null : Timestamp.from(record.lockedUntil()))
                .addValue("expiresAt", Timestamp.from(record.expiresAt()))
                .addValue("version", record.version());
        try {
            jdbcTemplate.update(sql, params);
            return record;
        } catch (DuplicateKeyException ex) {
            throw ex;
        }
    }

    public Optional<IdempotencyRecord> findByScope(
            String applicationId, String actorId, String operationCode, String idempotencyKey) {
        return jdbcTemplate
                .query(
                        """
                        SELECT * FROM idempotency.idempotency_records
                        WHERE COALESCE(application_id, '') = COALESCE(:applicationId, '')
                          AND COALESCE(actor_id, '') = COALESCE(:actorId, '')
                          AND operation_code = :operationCode
                          AND idempotency_key = :idempotencyKey
                        """,
                        scopeParams(applicationId, actorId, operationCode, idempotencyKey),
                        (rs, rowNum) -> mapRow(rs))
                .stream()
                .findFirst();
    }

    public Optional<IdempotencyRecord> findById(UUID id) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM idempotency.idempotency_records WHERE id = :id",
                        Map.of("id", id),
                        (rs, rowNum) -> mapRow(rs))
                .stream()
                .findFirst();
    }

    public void markCompleted(
            UUID id, long expectedVersion, int responseStatus, String responseContentType, Map<String, Object> body) {
        int updated = jdbcTemplate.update(
                """
                UPDATE idempotency.idempotency_records
                SET processing_status = 'COMPLETED',
                    response_status = :responseStatus,
                    response_content_type = :responseContentType,
                    response_body = CAST(:responseBody AS jsonb),
                    completed_at = (NOW() AT TIME ZONE 'UTC'),
                    updated_at = (NOW() AT TIME ZONE 'UTC'),
                    locked_until = NULL,
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("responseStatus", responseStatus)
                        .addValue("responseContentType", responseContentType)
                        .addValue("responseBody", toJson(body)));
        if (updated == 0) {
            throw new OptimisticLockException("Idempotency record version conflict");
        }
    }

    public void markFailedFinal(UUID id, long expectedVersion, String failureCode) {
        int updated = jdbcTemplate.update(
                """
                UPDATE idempotency.idempotency_records
                SET processing_status = 'FAILED_FINAL',
                    failure_code = :failureCode,
                    completed_at = (NOW() AT TIME ZONE 'UTC'),
                    updated_at = (NOW() AT TIME ZONE 'UTC'),
                    locked_until = NULL,
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("failureCode", failureCode));
        if (updated == 0) {
            throw new OptimisticLockException("Idempotency record version conflict");
        }
    }

    public void refreshLock(UUID id, long expectedVersion, Instant lockedUntil) {
        int updated = jdbcTemplate.update(
                """
                UPDATE idempotency.idempotency_records
                SET locked_until = :lockedUntil,
                    updated_at = (NOW() AT TIME ZONE 'UTC'),
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("lockedUntil", Timestamp.from(lockedUntil)));
        if (updated == 0) {
            throw new OptimisticLockException("Idempotency record version conflict");
        }
    }

    static MapSqlParameterSource scopeParams(
            String applicationId, String actorId, String operationCode, String idempotencyKey) {
        return new MapSqlParameterSource()
                .addValue("applicationId", applicationId)
                .addValue("actorId", actorId)
                .addValue("operationCode", operationCode)
                .addValue("idempotencyKey", idempotencyKey);
    }

    private MapSqlParameterSource baseParams(IdempotencyRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("applicationId", record.applicationId())
                .addValue("actorId", record.actorId())
                .addValue("idempotencyKey", record.idempotencyKey())
                .addValue("operationCode", record.operationCode())
                .addValue("requestHash", record.requestHash())
                .addValue("requestContentType", record.requestContentType())
                .addValue("resourceType", record.resourceType())
                .addValue("resourceId", record.resourceId());
    }

    private IdempotencyRecord mapRow(ResultSet rs) throws SQLException {
        return new IdempotencyRecord(
                rs.getObject("id", UUID.class),
                rs.getString("application_id"),
                rs.getString("actor_id"),
                rs.getString("idempotency_key"),
                rs.getString("operation_code"),
                rs.getString("request_hash"),
                rs.getString("request_content_type"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                IdempotencyStatus.valueOf(rs.getString("processing_status")),
                (Integer) rs.getObject("response_status"),
                rs.getString("response_content_type"),
                fromJson(rs.getString("response_body")),
                rs.getString("failure_code"),
                rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"));
    }

    private String toJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize idempotency response body", ex);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deserialize idempotency response body", ex);
        }
    }
}
