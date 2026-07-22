package ke.securepay.platform.persistence.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

class IdempotencyRepositoryScopeParamsTest {

    @Test
    void scopeParamsAcceptNullableApplicationAndActorIds() {
        MapSqlParameterSource params =
                IdempotencyRepository.scopeParams(null, "test-actor", "platform.technical.test", "key-001");

        assertThat(params.getValue("applicationId")).isNull();
        assertThat(params.getValue("actorId")).isEqualTo("test-actor");
        assertThat(params.getValue("operationCode")).isEqualTo("platform.technical.test");
        assertThat(params.getValue("idempotencyKey")).isEqualTo("key-001");
    }

    @Test
    void scopeParamsDoNotThrowWhenActorIdIsNull() {
        assertThatCode(() -> IdempotencyRepository.scopeParams("app-1", null, "platform.technical.test", "key-002"))
                .doesNotThrowAnyException();
    }
}
