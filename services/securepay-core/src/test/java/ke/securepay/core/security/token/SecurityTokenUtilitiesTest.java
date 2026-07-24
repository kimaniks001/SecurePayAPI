package ke.securepay.core.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecurityTokenUtilitiesTest {

    @Test
    void generatesDistinctUrlSafeOpaqueTokens() {
        SecureOpaqueTokenGenerator generator =
                new SecureOpaqueTokenGenerator();

        String first = generator.generate();
        String second = generator.generate();

        assertThat(first)
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
        assertThat(second)
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+")
                .isNotEqualTo(first);
    }

    @Test
    void producesStableSha256DigestWithoutRetainingRawToken() {
        Sha256TokenDigester digester = new Sha256TokenDigester();

        String digest = digester.digest("opaque-token");

        assertThat(digest)
                .hasSize(64)
                .matches("[0-9a-f]+")
                .isEqualTo(digester.digest("opaque-token"))
                .doesNotContain("opaque-token");
    }

    @Test
    void rejectsNullTokenForDigesting() {
        Sha256TokenDigester digester = new Sha256TokenDigester();

        assertThatThrownBy(() -> digester.digest(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("token");
    }
}
