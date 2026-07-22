package ke.securepay.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SecurePayProperties.class, ShutdownProperties.class})
public class SecurePayConfigurationPropertiesConfig {}
