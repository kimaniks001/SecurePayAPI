package ke.securepay.core;

import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SecurePayIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurepayCoreContextTest {

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void contextStarts() {}
}
