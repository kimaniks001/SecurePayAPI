package ke.securepay.core.security.token;

/**
 * Produces a stable one-way digest for opaque authentication tokens.
 */
public interface TokenDigester {

    String digest(String token);
}
