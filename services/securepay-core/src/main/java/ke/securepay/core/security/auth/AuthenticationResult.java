package ke.securepay.core.security.auth;

/**
 * Result returned after successful credential verification.
 *
 * OTP verification and session establishment are performed
 * in later phases.
 */
public record AuthenticationResult(
        AuthenticationPrincipal principal,
        boolean otpRequired) {
}
