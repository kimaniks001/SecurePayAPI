package ke.securepay.core.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import ke.securepay.platform.testing.containers.SecurePayTestImages;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return SecurePayTestImages.postgres();
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return SecurePayTestImages.redis();
    }
}
