package ke.securepay.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "securepay.shutdown")
public record ShutdownProperties(
        @Min(1) @Max(120) int timeoutSeconds) {

    public ShutdownProperties {
        if (timeoutSeconds == 0) {
            timeoutSeconds = 30;
        }
    }
}
