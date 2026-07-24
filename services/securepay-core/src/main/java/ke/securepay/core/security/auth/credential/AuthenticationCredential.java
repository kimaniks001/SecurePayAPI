package ke.securepay.core.security.auth.credential;

import java.util.Objects;

/**
 * Credential data required to verify a SecurePay identity.
 *
 * Password hashes must never be exposed through API responses.
 */
public record AuthenticationCredential(
        String actorId,
        String ksNumber,
        String displayName,
        String passwordHash,
        boolean active) {

    public AuthenticationCredential {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(ksNumber, "ksNumber");
        Objects.requireNonNull(passwordHash, "passwordHash");
    }
}
