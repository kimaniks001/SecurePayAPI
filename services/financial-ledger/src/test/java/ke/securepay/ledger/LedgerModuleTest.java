package ke.securepay.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LedgerModuleTest {

    @Test
    void moduleCompilesAndLoads() {
        assertThat(LedgerModule.MODULE_NAME).isEqualTo("financial-ledger");
    }
}
