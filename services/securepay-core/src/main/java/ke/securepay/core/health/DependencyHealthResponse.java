package ke.securepay.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DependencyHealthResponse(
        @JsonProperty("name") String name,
        @JsonProperty("status") String status,
        @JsonProperty("latency_ms") Long latencyMs,
        @JsonProperty("message") String message) {

    public static DependencyHealthResponse of(String name, DependencyStatus status, Long latencyMs, String message) {
        return new DependencyHealthResponse(name, status.apiValue(), latencyMs, message);
    }
}
