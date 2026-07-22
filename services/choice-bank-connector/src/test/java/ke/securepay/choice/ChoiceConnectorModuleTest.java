package ke.securepay.choice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChoiceConnectorModuleTest {

    @Test
    void moduleCompilesAndLoads() {
        assertThat(ChoiceConnectorModule.MODULE_NAME).isEqualTo("choice-bank-connector");
    }
}
