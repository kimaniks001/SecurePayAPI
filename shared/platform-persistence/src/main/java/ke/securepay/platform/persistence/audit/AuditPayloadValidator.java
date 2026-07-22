package ke.securepay.platform.persistence.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Rejects secrets and unsafe audit payload keys. */
public final class AuditPayloadValidator {

    private static final Set<String> FORBIDDEN_KEYS =
            Set.of("password", "secret", "token", "api_key", "apikey", "authorization", "otp", "pin");

    private final ObjectMapper objectMapper;

    public AuditPayloadValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> sanitize(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        JsonNode node = objectMapper.valueToTree(payload);
        validateNode(node, "");
        return payload;
    }

    private void validateNode(JsonNode node, String path) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                if (FORBIDDEN_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Audit payload contains forbidden key: " + path + key);
                }
                validateNode(entry.getValue(), path + key + ".");
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                validateNode(node.get(i), path + i + ".");
            }
        }
    }
}
