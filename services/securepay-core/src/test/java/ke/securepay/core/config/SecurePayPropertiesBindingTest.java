package ke.securepay.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class SecurePayPropertiesBindingTest {

    @Test
    void bindsRequiredSecurePayProperties() {
        var source = new MapConfigurationPropertySource();
        source.put("securepay.application.name", "securepay-core");
        source.put("securepay.application.environment", "test");
        source.put("securepay.api.version", "1.0.0");

        var binder = new Binder(source);
        SecurePayProperties properties =
                binder.bind("securepay", Bindable.of(SecurePayProperties.class)).get();

        assertThat(properties.application().name()).isEqualTo("securepay-core");
        assertThat(properties.api().version()).isEqualTo("1.0.0");
    }
}
