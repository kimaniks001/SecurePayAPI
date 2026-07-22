package ke.securepay.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LiveHealthData(@JsonProperty("status") String status) {
    public static LiveHealthData alive() {
        return new LiveHealthData("alive");
    }
}
