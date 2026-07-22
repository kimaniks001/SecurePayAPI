package ke.securepay.platform.persistence.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.testing.contracts.EventEnvelopeSchemaSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DomainEventEnvelopeContractTest {

    private static JsonSchema schema;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void loadSchema() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        schema = EventEnvelopeSchemaSupport.loadSchema();
    }

    @Test
    void technicalTestEventSerializesToValidEnvelope() throws Exception {
        ActorContext actor = ActorContextFactory.test("securepay-core");
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                "evt_test_12345678",
                TechnicalEventTypes.PLATFORM_TEST_CREATED,
                TechnicalEventTypes.EVENT_VERSION,
                Instant.parse("2026-07-23T09:00:00Z"),
                actor.correlationId(),
                null,
                actor.sourceService(),
                new DomainEventEnvelope.Actor(actor.actorType().apiValue(), actor.actorId(), null),
                new DomainEventEnvelope.Resource("technical_test", "record-001", "1"),
                Map.of("phase", "3"),
                Map.of("non_sensitive", true));

        String json = objectMapper.writeValueAsString(envelope);
        Set<ValidationMessage> errors = schema.validate(objectMapper.readTree(json));
        assertThat(errors).isEmpty();
        assertThat(json).doesNotContain("ke.securepay");
    }
}
