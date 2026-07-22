package ke.securepay.core.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import ke.securepay.platform.testing.contracts.EventEnvelopeSchemaSupport;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SecurePayIntegrationTest
class IntegrationTestInfrastructureTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void eventEnvelopeSchemaIsAvailableOnClasspath() {
        assertThat(EventEnvelopeSchemaSupport.loadSchemaJson()).isNotBlank();
    }

    @Test
    void singlePostgresAndRedisTestcontainersAreRegistered() {
        assertThat(applicationContext.getBeansOfType(PostgreSQLContainer.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(GenericContainer.class)).hasSize(1);
        assertThat(applicationContext.containsBean("postgresContainer")).isTrue();
        assertThat(applicationContext.containsBean("redisContainer")).isTrue();
    }

    @Test
    void datasourceAndRedisConnectionsAreAvailable() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
        assertThat(redisConnectionFactory.getConnection().ping()).isEqualTo("PONG");
    }
}
