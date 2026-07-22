package ke.securepay.platform.persistence.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditEventRepository {

    private static final String INSERT_SQL =
            """
            INSERT INTO audit.audit_events (
                id, event_id, category, event_type, actor_type, actor_id, actor_ks_number,
                application_id, resource_type, resource_id, action, previous_state, new_state,
                reason, request_id, correlation_id, source_service, occurred_at, created_at, metadata,
                integrity_version, integrity_hash
            ) VALUES (
                :id, :eventId, :category, :eventType, :actorType, :actorId, :actorKsNumber,
                :applicationId, :resourceType, :resourceId, :action,
                CAST(:previousState AS jsonb), CAST(:newState AS jsonb),
                :reason, :requestId, :correlationId, :sourceService, :occurredAt, :createdAt,
                CAST(:metadata AS jsonb), :integrityVersion, :integrityHash
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditEventRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void append(AuditEventRecord record) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("eventId", record.eventId())
                .addValue("category", record.category())
                .addValue("eventType", record.eventType())
                .addValue("actorType", record.actorType())
                .addValue("actorId", record.actorId())
                .addValue("actorKsNumber", record.actorKsNumber())
                .addValue("applicationId", record.applicationId())
                .addValue("resourceType", record.resourceType())
                .addValue("resourceId", record.resourceId())
                .addValue("action", record.action())
                .addValue("previousState", toJson(record.previousState()))
                .addValue("newState", toJson(record.newState()))
                .addValue("reason", record.reason())
                .addValue("requestId", record.requestId())
                .addValue("correlationId", record.correlationId())
                .addValue("sourceService", record.sourceService())
                .addValue("occurredAt", Timestamp.from(record.occurredAt()))
                .addValue("createdAt", Timestamp.from(record.createdAt()))
                .addValue("metadata", toJson(record.metadata()))
                .addValue("integrityVersion", record.integrityVersion())
                .addValue("integrityHash", record.integrityHash());
        jdbcTemplate.update(INSERT_SQL, params);
    }

    public Optional<AuditEventRecord> findByEventId(String eventId) {
        return jdbcTemplate.query(
                        "SELECT * FROM audit.audit_events WHERE event_id = :eventId",
                        Map.of("eventId", eventId),
                        (rs, rowNum) -> mapRow(rs))
                .stream()
                .findFirst();
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit.audit_events", Map.of(), Long.class);
        return count == null ? 0L : count;
    }

    private AuditEventRecord mapRow(ResultSet rs) throws SQLException {
        return new AuditEventRecord(
                rs.getObject("id", UUID.class),
                rs.getString("event_id"),
                rs.getString("category"),
                rs.getString("event_type"),
                rs.getString("actor_type"),
                rs.getString("actor_id"),
                rs.getString("actor_ks_number"),
                rs.getString("application_id"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                rs.getString("action"),
                fromJson(rs.getString("previous_state")),
                fromJson(rs.getString("new_state")),
                rs.getString("reason"),
                rs.getString("request_id"),
                rs.getString("correlation_id"),
                rs.getString("source_service"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                fromJson(rs.getString("metadata")),
                rs.getInt("integrity_version"),
                rs.getString("integrity_hash"));
    }

    private String toJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize audit JSON payload", ex);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deserialize audit JSON payload", ex);
        }
    }
}
