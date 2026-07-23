package ke.securepay.core.security.password;

import java.util.Objects;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt implementation isolated behind SecurePay password abstractions.
 */
@Component
public final class BCryptPasswordHasher
        implements PasswordHasher, PasswordVerifier {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordHasher() {
        this(new BCryptPasswordEncoder());
    }

    BCryptPasswordHasher(BCryptPasswordEncoder encoder) {
        this.encoder = Objects.requireNonNull(encoder, "encoder");
    }

    @Override
    public String hash(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(passwordHash, "passwordHash");
        return encoder.matches(rawPassword, passwordHash);
    }
}
