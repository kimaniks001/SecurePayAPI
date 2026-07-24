package ke.securepay.core.security.token;

/**
 * Generates cryptographically strong opaque authentication secrets.
 */
public interface OpaqueTokenGenerator {

    String generate();
}
