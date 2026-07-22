package ke.securepay.platform.testing.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Shared Testcontainers image references for SecurePay integration tests. */
public final class SecurePayTestImages {

    public static final DockerImageName POSTGRES = DockerImageName.parse("postgres:16-alpine");
    public static final DockerImageName REDIS = DockerImageName.parse("redis:7-alpine");

    private SecurePayTestImages() {}

    public static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(POSTGRES)
                .withDatabaseName("securepay_test")
                .withUsername("securepay_test")
                .withPassword("securepay_test_password");
    }

    @SuppressWarnings("resource")
    public static GenericContainer<?> redis() {
        return new GenericContainer<>(REDIS).withExposedPorts(6379);
    }
}
