package ke.securepay.core.security.auth;

import java.util.Objects;

/**
 * Represents an authenticated SecurePay identity.
 *
 * This model is independent of HTTP, Spring Security,
 * JWT, sessions and OTP transport.
 */
public record AuthenticationPrincipal(
        String actorId,
        String ksNumber,
        String displayName,
        String authenticationMethod,
        String applicationId,
        String deviceId,
        String sourceIpHash) {

    public AuthenticationPrincipal {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(ksNumber, "ksNumber");
        Objects.requireNonNull(authenticationMethod, "authenticationMethod");
    }
}
