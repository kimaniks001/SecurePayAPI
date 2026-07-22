package ke.securepay.platform.persistence.technical;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.audit.AuditCategory;
import ke.securepay.platform.persistence.audit.AuditEventRecord;
import ke.securepay.platform.persistence.audit.AuditWriter;
import ke.securepay.platform.persistence.outbox.OutboxEventRecord;
import ke.securepay.platform.persistence.outbox.OutboxService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnicalFoundationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuditWriter auditWriter;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public TechnicalFoundationService(
            NamedParameterJdbcTemplate jdbcTemplate,
            AuditWriter auditWriter,
            OutboxService outboxService,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditWriter = auditWriter;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TechnicalFlowResult recordTechnicalCreation(ActorContext actor, String recordKey, Map<String, Object> payload) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO platform.technical_test_records (id, record_key, payload)
                VALUES (:id, :recordKey, CAST(:payload AS jsonb))
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("recordKey", recordKey)
                        .addValue("payload", toJson(payload)));

        AuditEventRecord audit = auditWriter.append(
                AuditCategory.PLATFORM_TECHNICAL,
                "platform.technical.test.created",
                actor,
                "technical_test",
                recordKey,
                "create",
                null,
                payload,
                "Phase 3 technical test flow",
                Map.of("record_id", id.toString()));

        OutboxEventRecord outbox = outboxService.appendTechnicalTestCreated(actor, recordKey, payload, audit.eventId());
        return new TechnicalFlowResult(id, audit, outbox);
    }

    public boolean technicalRecordExists(String recordKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM platform.technical_test_records WHERE record_key = :recordKey",
                Map.of("recordKey", recordKey),
                Integer.class);
        return count != null && count > 0;
    }

    public long countOutboxEventsForAggregate(String aggregateId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events.outbox_events WHERE aggregate_id = :aggregateId",
                Map.of("aggregateId", aggregateId),
                Long.class);
        return count == null ? 0L : count;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize technical test payload", ex);
        }
    }

    public record TechnicalFlowResult(UUID recordId, AuditEventRecord auditEvent, OutboxEventRecord outboxEvent) {}
}
