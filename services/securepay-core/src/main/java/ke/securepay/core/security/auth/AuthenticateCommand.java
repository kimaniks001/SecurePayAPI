package ke.securepay.core.security.auth;

import java.util.Objects;

/**
 * Credentials and request context submitted for authentication.
 */
public record AuthenticateCommand(
        String ksNumber,
        String password,
        String applicationId,
        String deviceId,
        String sourceIpHash) {

    public AuthenticateCommand {
        Objects.requireNonNull(ksNumber, "ksNumber");
        Objects.requireNonNull(password, "password");
    }
}
