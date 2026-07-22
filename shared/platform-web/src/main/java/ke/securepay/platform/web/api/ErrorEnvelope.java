package ke.securepay.platform.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorEnvelope(
        @JsonProperty("success") boolean success,
        @JsonProperty("error") ErrorBody error,
        @JsonProperty("meta") ResponseMeta meta) {

    public static ErrorEnvelope of(ErrorBody error, ResponseMeta meta) {
        return new ErrorEnvelope(false, error, meta);
    }
}
