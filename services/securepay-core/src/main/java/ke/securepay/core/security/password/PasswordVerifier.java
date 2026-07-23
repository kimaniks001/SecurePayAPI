package ke.securepay.core.security.password;

/**
 * Verifies a raw password against a stored password hash.
 */
public interface PasswordVerifier {

    boolean matches(String rawPassword, String passwordHash);
}
