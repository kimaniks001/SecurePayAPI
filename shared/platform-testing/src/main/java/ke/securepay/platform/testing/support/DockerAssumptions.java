package ke.securepay.platform.testing.support;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.DockerClientFactory;

public final class DockerAssumptions {

    public static final String REQUIRE_TESTCONTAINERS_ENV = "SECUREPAY_REQUIRE_TESTCONTAINERS";

    private DockerAssumptions() {}

    /**
     * Enforces the SecurePay Testcontainers policy for integration tests.
     *
     * <p>When {@value #REQUIRE_TESTCONTAINERS_ENV} is {@code true}, Docker must be available and
     * Testcontainer-backed tests must run (CI). When false, tests skip with a clear reason if Docker
     * is unavailable (local development without Docker).
     */
    public static void enforceDockerPolicyForIntegrationTests() {
        boolean required = isTestcontainersRequired();
        boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();

        if (required) {
            Assertions.assertTrue(
                    dockerAvailable,
                    REQUIRE_TESTCONTAINERS_ENV
                            + "=true but Docker is not available. PostgreSQL and Redis Testcontainer"
                            + " tests must run and cannot be skipped.");
            return;
        }

        Assumptions.assumeTrue(
                dockerAvailable,
                "Docker is not available; skipping integration tests. Start Docker locally or set "
                        + REQUIRE_TESTCONTAINERS_ENV
                        + "=true to fail when Docker is required.");
    }

    public static boolean isTestcontainersRequired() {
        return Boolean.parseBoolean(System.getenv().getOrDefault(REQUIRE_TESTCONTAINERS_ENV, "false"));
    }
}
