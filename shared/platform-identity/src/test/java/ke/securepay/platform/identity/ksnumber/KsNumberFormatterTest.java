package ke.securepay.platform.identity.ksnumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KsNumberFormatterTest {

    @Test
    void formatsSequenceOneAsKs001() {
        assertThat(KsNumberFormatter.format(1)).isEqualTo("KS001");
    }

    @Test
    void formatsSequenceNineAsKs009() {
        assertThat(KsNumberFormatter.format(9)).isEqualTo("KS009");
    }

    @Test
    void formatsSequenceTenAsKs010() {
        assertThat(KsNumberFormatter.format(10)).isEqualTo("KS010");
    }

    @Test
    void formatsSequence999AsKs999() {
        assertThat(KsNumberFormatter.format(999)).isEqualTo("KS999");
    }

    @Test
    void formatsSequence1000AsKs1000() {
        assertThat(KsNumberFormatter.format(1000)).isEqualTo("KS1000");
    }

    @Test
    void rejectsZero() {
        assertThatThrownBy(() -> KsNumberFormatter.format(0))
                .isInstanceOf(InvalidKsNumberException.class);
    }

    @Test
    void rejectsNegativeValues() {
        assertThatThrownBy(() -> KsNumberFormatter.format(-1))
                .isInstanceOf(InvalidKsNumberException.class);
    }
}
