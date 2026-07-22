package ke.securepay.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadyHealthData(@JsonProperty("status") String status) {
    public static ReadyHealthData ready() {
        return new ReadyHealthData("ready");
    }

    public static ReadyHealthData notReady() {
        return new ReadyHealthData("not_ready");
    }
}
