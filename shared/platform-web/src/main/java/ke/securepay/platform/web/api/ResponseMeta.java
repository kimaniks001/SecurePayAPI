package ke.securepay.platform.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseMeta(
        @JsonProperty("request_id") String requestId,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("api_version") String apiVersion,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("pagination") PaginationMeta pagination) {

    public static ResponseMeta of(String requestId, String correlationId, String apiVersion, Instant timestamp) {
        return new ResponseMeta(requestId, correlationId, apiVersion, timestamp, null);
    }
}
