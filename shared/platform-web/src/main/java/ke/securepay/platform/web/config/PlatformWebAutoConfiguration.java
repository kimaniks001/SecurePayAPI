package ke.securepay.platform.web.config;

import ke.securepay.platform.common.time.ClockProvider;
import ke.securepay.platform.common.time.SystemClockProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "ke.securepay.platform.web")
public class PlatformWebAutoConfiguration {

    @Bean
    ClockProvider clockProvider() {
        return new SystemClockProvider();
    }
}
