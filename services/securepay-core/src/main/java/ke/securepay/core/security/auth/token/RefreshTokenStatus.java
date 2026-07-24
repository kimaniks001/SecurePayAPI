package ke.securepay.core.security.auth.token;

public enum RefreshTokenStatus {
    ACTIVE,
    ROTATED,
    EXPIRED,
    REVOKED,
    REPLAYED
}
