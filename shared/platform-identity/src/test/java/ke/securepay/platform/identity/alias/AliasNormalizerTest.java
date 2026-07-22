package ke.securepay.platform.identity.alias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AliasNormalizerTest {

    @Test
    void normalizesCaseInsensitively() {
        var normalized = AliasNormalizer.normalizeOrThrow("My.Alias");
        assertThat(normalized.normalizedAlias()).isEqualTo("my.alias");
    }

    @Test
    void rejectsCanonicalLookingAlias() {
        assertThatThrownBy(() -> AliasNormalizer.normalizeOrThrow("KS001"))
                .isInstanceOf(InvalidAliasException.class);
    }

    @Test
    void rejectsReservedTerms() {
        assertThatThrownBy(() -> AliasNormalizer.normalizeOrThrow("admin"))
                .isInstanceOf(InvalidAliasException.class);
    }
}
