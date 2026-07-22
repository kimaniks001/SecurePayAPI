package ke.securepay.platform.testing.contracts;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/** Loads the authoritative event envelope JSON Schema from the test classpath. */
public final class EventEnvelopeSchemaSupport {

    public static final String CLASSPATH_LOCATION = "contracts/events/event-envelope-v1.schema.json";

    private EventEnvelopeSchemaSupport() {}

    public static String loadSchemaJson() {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_LOCATION);
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Event envelope schema is not on the test classpath at "
                            + CLASSPATH_LOCATION
                            + ". Ensure the build copies contracts/events/event-envelope-v1.schema.json"
                            + " into test resources.");
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read event envelope schema from classpath", exception);
        }
    }

    public static JsonSchema loadSchema() {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(loadSchemaJson());
    }
}
