package ke.securepay.platform.persistence.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DomainEventEnvelope(
        String event_id,
        String event_type,
        String event_version,
        Instant occurred_at,
        String correlation_id,
        String causation_id,
        String source,
        Actor actor,
        Resource resource,
        Map<String, Object> data,
        Map<String, Object> metadata) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Actor(String type, String id, String ks_number) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resource(String type, String id, String version) {}
}
