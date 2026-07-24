package ke.securepay.core.security.token;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * SecureRandom-backed opaque authentication token generator.
 */
@Component
public final class SecureOpaqueTokenGenerator
        implements OpaqueTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureOpaqueTokenGenerator() {
        this(new SecureRandom());
    }

    SecureOpaqueTokenGenerator(SecureRandom secureRandom) {
        this.secureRandom =
                Objects.requireNonNull(secureRandom, "secureRandom");
    }

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
