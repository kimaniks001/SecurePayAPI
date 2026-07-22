package ke.securepay.platform.identity.issuance;

import ke.securepay.platform.persistence.idempotency.IdempotencyKeyValidator;

/** Immutable fingerprint of an identity issuance command payload. */
public final class IssuanceRequestFingerprint {

    private IssuanceRequestFingerprint() {}

    public static String hash(String requestBody) {
        return IdempotencyKeyValidator.hashRequest(requestBody);
    }
}
