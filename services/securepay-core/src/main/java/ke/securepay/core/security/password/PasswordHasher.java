package ke.securepay.core.security.password;

/**
 * Creates a secure one-way password hash.
 */
public interface PasswordHasher {

    String hash(String rawPassword);
}
