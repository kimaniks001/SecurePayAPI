package ke.securepay.core.health;

import static org.assertj.core.api.Assertions.assertThat;

import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.common.ids.IdentifierRules;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SecurePayIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void liveEndpointReturnsSuccessEnvelope() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health/live", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"success\":true").contains("\"status\":\"alive\"");
        assertThat(response.getHeaders().getFirst(IdentifierRules.REQUEST_HEADER)).isNotBlank();
        assertThat(response.getHeaders().getFirst(IdentifierRules.CORRELATION_HEADER)).isNotBlank();
    }

    @Test
    void readyEndpointReturnsReadyWhenDependenciesAreHealthy() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health/ready", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"ready\"");
    }

    @Test
    void dependenciesEndpointReportsHealthyPostgresRedisAndApplication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health/dependencies", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body)
                .contains("\"name\":\"postgres\"")
                .contains("\"name\":\"redis\"")
                .contains("\"name\":\"application\"")
                .contains("\"status\":\"healthy\"")
                .doesNotContain("\"status\":\"unhealthy\"")
                .doesNotContain("\"status\":\"unknown\"");
    }

    @Test
    void dependencyStatusesUseApprovedPublicVocabularyOnly() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health/dependencies", String.class);

        String body = response.getBody();
        assertThat(body).containsAnyOf("\"status\":\"healthy\"", "\"status\":\"degraded\"", "\"status\":\"unavailable\"");
        assertThat(body).doesNotContain("\"status\":\"unhealthy\"");
    }

    @Test
    void correlationIdIsPropagatedWhenValid() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IdentifierRules.CORRELATION_HEADER, "corr_integration01");
        ResponseEntity<String> response =
                restTemplate.exchange("/health/live", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(IdentifierRules.CORRELATION_HEADER))
                .isEqualTo("corr_integration01");
    }

    @Test
    void invalidCorrelationIdReturnsSafeErrorEnvelope() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IdentifierRules.CORRELATION_HEADER, "bad id!");
        ResponseEntity<String> response =
                restTemplate.exchange("/health/live", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"success\":false").contains("INVALID_CORRELATION_ID");
    }
}
