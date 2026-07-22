package ke.securepay.core.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DependencyStatusContractTest {

    @Test
    void publicApiValuesUseApprovedVocabulary() {
        assertThat(DependencyStatus.HEALTHY.apiValue()).isEqualTo("healthy");
        assertThat(DependencyStatus.DEGRADED.apiValue()).isEqualTo("degraded");
        assertThat(DependencyStatus.UNAVAILABLE.apiValue()).isEqualTo("unavailable");
    }

    @Test
    void publicApiValuesDoNotUseDeprecatedUnhealthyLabel() {
        for (DependencyStatus status : DependencyStatus.values()) {
            assertThat(status.apiValue()).isNotEqualTo("unhealthy");
            assertThat(status.apiValue()).isNotEqualTo("unknown");
        }
    }
}
