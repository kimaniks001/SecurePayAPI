package ke.securepay.core.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import ke.securepay.platform.testing.containers.SecurePayTestImages;
import ke.securepay.platform.testing.contracts.EventEnvelopeSchemaSupport;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        assertThat(applicationContext.containsBean("postgresContainer")).isTrue();
        assertThat(applicationContext.containsBean("redisContainer")).isTrue();

        Object postgresBean = applicationContext.getBean("postgresContainer");
        Object redisBean = applicationContext.getBean("redisContainer");

        assertThat(postgresBean).isInstanceOf(PostgreSQLContainer.class);
        assertThat(redisBean).isInstanceOf(GenericContainer.class);

        PostgreSQLContainer<?> postgres = (PostgreSQLContainer<?>) postgresBean;
        GenericContainer<?> redis = (GenericContainer<?>) redisBean;

        assertThat(postgres.getDockerImageName())
                .isEqualTo(SecurePayTestImages.POSTGRES.asCanonicalNameString());
        assertThat(redis.getDockerImageName())
                .isEqualTo(SecurePayTestImages.REDIS.asCanonicalNameString());
    }

    @Test
    void datasourceAndRedisConnectionsAreAvailable() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
        assertThat(redisConnectionFactory.getConnection().ping()).isEqualTo("PONG");
    }
}
