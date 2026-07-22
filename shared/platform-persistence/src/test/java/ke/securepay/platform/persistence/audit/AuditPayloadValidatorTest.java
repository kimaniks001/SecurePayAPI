package ke.securepay.platform.persistence.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditPayloadValidatorTest {

    @Test
    void rejectsForbiddenSecretKeys() {
        AuditPayloadValidator validator = new AuditPayloadValidator(new ObjectMapper());
        assertThatThrownBy(() -> validator.sanitize(Map.of("token", "abc")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
