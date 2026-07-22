package ke.securepay.core.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import ke.securepay.core.SecurepayCoreApplication;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.testing.containers.SecurePayTestImages;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = SecurepayCoreApplication.class)
@ActiveProfiles("test")
@Testcontainers
@Import(IdentityFreshDatabaseIntegrationTest.FreshContainersConfig.class)
class IdentityFreshDatabaseIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = SecurePayTestImages.postgres();

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis = SecurePayTestImages.redis();

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Autowired
    private KsIdentityIssuanceService issuanceService;

    @Test
    void firstIdentityOnEmptyDatabaseIsKs001() {
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "fresh-db-first-" + UUID.randomUUID(), IdentityType.TEST, "First", actor));

        assertThat(issued.canonicalKsNumber().canonicalValue()).isEqualTo("KS001");
        assertThat(issued.sequenceNumber()).isEqualTo(1L);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FreshContainersConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return postgres;
        }

        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redisContainer() {
            return redis;
        }
    }
}
