package ke.securepay.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "securepay")
public record SecurePayProperties(@Valid @NotNull Application application, @Valid @NotNull Api api) {

    @Validated
    public record Application(@NotBlank String name, @NotBlank String environment) {}

    @Validated
    public record Api(@NotBlank String version) {}
}
