package ke.securepay.core.security.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Returned after password verification creates a persistent MFA challenge.
 *
 * The raw challenge token is returned once to the caller. Only its digest
 * is persisted.
 */
public record PendingAuthenticationResult(
        UUID challengeId,
        String challengeToken,
        Instant expiresAt,
        AuthenticationPrincipal principal) {

    public PendingAuthenticationResult {
        Objects.requireNonNull(challengeId, "challengeId");
        Objects.requireNonNull(challengeToken, "challengeToken");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(principal, "principal");
    }
}
