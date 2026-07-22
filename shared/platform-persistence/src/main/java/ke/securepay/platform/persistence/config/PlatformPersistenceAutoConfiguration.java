package ke.securepay.platform.persistence.config;

import java.time.Clock;
import ke.securepay.platform.common.time.ClockProvider;
import ke.securepay.platform.persistence.audit.AuditPayloadValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfiguration
@ComponentScan(basePackages = "ke.securepay.platform.persistence")
public class PlatformPersistenceAutoConfiguration {

    @Bean
    Clock clock(ClockProvider clockProvider) {
        return clockProvider.clock();
    }

    @Bean
    AuditPayloadValidator auditPayloadValidator(ObjectMapper objectMapper) {
        return new AuditPayloadValidator(objectMapper);
    }
}
