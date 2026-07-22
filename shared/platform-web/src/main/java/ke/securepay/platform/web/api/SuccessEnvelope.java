package ke.securepay.platform.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessEnvelope<T>(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") T data,
        @JsonProperty("meta") ResponseMeta meta) {

    public static <T> SuccessEnvelope<T> of(T data, ResponseMeta meta) {
        return new SuccessEnvelope<>(true, data, meta);
    }
}
