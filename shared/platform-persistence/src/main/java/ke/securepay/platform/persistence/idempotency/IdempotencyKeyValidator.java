package ke.securepay.platform.persistence.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import ke.securepay.platform.common.ids.IdentifierRules;

public final class IdempotencyKeyValidator {

    private IdempotencyKeyValidator() {}

    public static String normalizeOrThrow(String idempotencyKey) {
        if (!IdentifierRules.isValid(idempotencyKey)) {
            throw new IllegalArgumentException("Invalid idempotency key format");
        }
        return idempotencyKey.trim();
    }

    public static String hashRequest(String requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
