package ke.securepay.platform.identity.ksnumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KsNumberParserTest {

    @Test
    void parsesCanonicalValues() {
        KsNumber parsed = KsNumberParser.parse("KS001");
        assertThat(parsed.canonicalValue()).isEqualTo("KS001");
        assertThat(parsed.sequenceNumber()).isEqualTo(1L);
    }

    @Test
    void rejectsLowercaseInput() {
        assertThatThrownBy(() -> KsNumberParser.parse("ks001"))
                .isInstanceOf(InvalidKsNumberException.class);
    }

    @Test
    void rejectsKs000() {
        assertThatThrownBy(() -> KsNumberParser.parse("KS000"))
                .isInstanceOf(InvalidKsNumberException.class);
    }

    @Test
    void rejectsMalformedValues() {
        assertThatThrownBy(() -> KsNumberParser.parse("KS-001"))
                .isInstanceOf(InvalidKsNumberException.class);
        assertThatThrownBy(() -> KsNumberParser.parse("KS 001"))
                .isInstanceOf(InvalidKsNumberException.class);
        assertThatThrownBy(() -> KsNumberParser.parse("AB001"))
                .isInstanceOf(InvalidKsNumberException.class);
    }

    @Test
    void equalityUsesCanonicalValueAndSequence() {
        assertThat(KsNumber.parse("KS010")).isEqualTo(KsNumber.fromSequence(10));
    }
}
