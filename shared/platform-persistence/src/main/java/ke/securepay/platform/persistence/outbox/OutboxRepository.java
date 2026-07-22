package ke.securepay.platform.persistence.outbox;

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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void insert(OutboxEventRecord record) {
        String sql =
                """
                INSERT INTO events.outbox_events (
                    id, event_id, aggregate_type, aggregate_id, event_type, event_version,
                    payload, metadata, correlation_id, causation_id, actor_type, actor_id,
                    source_service, status, available_at, attempt_count, version
                ) VALUES (
                    :id, :eventId, :aggregateType, :aggregateId, :eventType, :eventVersion,
                    CAST(:payload AS jsonb), CAST(:metadata AS jsonb), :correlationId, :causationId,
                    :actorType, :actorId, :sourceService, :status, :availableAt, :attemptCount, :version
                )
                """;
        jdbcTemplate.update(sql, params(record));
    }

    public Optional<OutboxEventRecord> findByEventId(String eventId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM events.outbox_events WHERE event_id = :eventId",
                        Map.of("eventId", eventId),
                        (rs, rowNum) -> mapRow(rs))
                .stream()
                .findFirst();
    }

    public void markProcessing(UUID id, long expectedVersion) {
        int updated = jdbcTemplate.update(
                """
                UPDATE events.outbox_events
                SET status = 'PROCESSING',
                    attempt_count = attempt_count + 1,
                    last_attempt_at = (NOW() AT TIME ZONE 'UTC'),
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                Map.of("id", id, "expectedVersion", expectedVersion));
        if (updated == 0) {
            throw new OptimisticLockException("Outbox record version conflict");
        }
    }

    public void markFailed(UUID id, long expectedVersion, String failureReason) {
        int updated = jdbcTemplate.update(
                """
                UPDATE events.outbox_events
                SET status = 'FAILED',
                    failure_reason = :failureReason,
                    last_attempt_at = (NOW() AT TIME ZONE 'UTC'),
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                Map.of("id", id, "expectedVersion", expectedVersion, "failureReason", failureReason));
        if (updated == 0) {
            throw new OptimisticLockException("Outbox record version conflict");
        }
    }

    public Optional<OutboxEventRecord> findById(UUID id) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM events.outbox_events WHERE id = :id",
                        Map.of("id", id),
                        (rs, rowNum) -> mapRow(rs))
                .stream()
                .findFirst();
    }

    public void markPublished(UUID id, long expectedVersion) {
        int updated = jdbcTemplate.update(
                """
                UPDATE events.outbox_events
                SET status = 'PUBLISHED',
                    published_at = (NOW() AT TIME ZONE 'UTC'),
                    failure_reason = NULL,
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                Map.of("id", id, "expectedVersion", expectedVersion));
        if (updated == 0) {
            throw new OptimisticLockException("Outbox record version conflict");
        }
    }

    public void markDeadLetter(UUID id, long expectedVersion, String failureReason) {
        int updated = jdbcTemplate.update(
                """
                UPDATE events.outbox_events
                SET status = 'DEAD_LETTER',
                    failure_reason = :failureReason,
                    version = version + 1
                WHERE id = :id AND version = :expectedVersion
                """,
                Map.of("id", id, "expectedVersion", expectedVersion, "failureReason", failureReason));
        if (updated == 0) {
            throw new OptimisticLockException("Outbox record version conflict");
        }
    }

    public long countByStatus(OutboxStatus status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events.outbox_events WHERE status = :status",
                Map.of("status", status.name()),
                Long.class);
        return count == null ? 0L : count;
    }

    private MapSqlParameterSource params(OutboxEventRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("eventId", record.eventId())
                .addValue("aggregateType", record.aggregateType())
                .addValue("aggregateId", record.aggregateId())
                .addValue("eventType", record.eventType())
                .addValue("eventVersion", record.eventVersion())
                .addValue("payload", toJson(record.payload()))
                .addValue("metadata", toJson(record.metadata()))
                .addValue("correlationId", record.correlationId())
                .addValue("causationId", record.causationId())
                .addValue("actorType", record.actorType())
                .addValue("actorId", record.actorId())
                .addValue("sourceService", record.sourceService())
                .addValue("status", record.status().name())
                .addValue("availableAt", Timestamp.from(record.availableAt()))
                .addValue("attemptCount", record.attemptCount())
                .addValue("version", record.version());
    }

    private OutboxEventRecord mapRow(ResultSet rs) throws SQLException {
        return new OutboxEventRecord(
                rs.getObject("id", UUID.class),
                rs.getString("event_id"),
                rs.getString("aggregate_type"),
                rs.getString("aggregate_id"),
                rs.getString("event_type"),
                rs.getString("event_version"),
                fromJson(rs.getString("payload")),
                fromJson(rs.getString("metadata")),
                rs.getString("correlation_id"),
                rs.getString("causation_id"),
                rs.getString("actor_type"),
                rs.getString("actor_id"),
                rs.getString("source_service"),
                OutboxStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("available_at").toInstant(),
                rs.getInt("attempt_count"),
                rs.getTimestamp("last_attempt_at") == null ? null : rs.getTimestamp("last_attempt_at").toInstant(),
                rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toInstant(),
                rs.getString("failure_reason"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getLong("version"));
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize outbox JSON payload", ex);
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deserialize outbox JSON payload", ex);
        }
    }
}
