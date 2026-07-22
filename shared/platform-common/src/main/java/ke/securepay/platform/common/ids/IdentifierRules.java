package ke.securepay.platform.common.ids;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

/** Generates and validates request and correlation identifiers. */
public final class IdentifierRules {

    public static final String REQUEST_HEADER = "X-Request-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private static final Pattern SAFE_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{7,127}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdentifierRules() {}

    public static String newRequestId() {
        return "req_" + randomSuffix();
    }

    public static String newCorrelationId() {
        return "corr_" + randomSuffix();
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            return false;
        }
        return SAFE_PATTERN.matcher(trimmed).matches();
    }

    public static String normalizeOrThrow(String candidate, String fieldName) {
        if (!isValid(candidate)) {
            throw new InvalidIdentifierException(fieldName);
        }
        return candidate.trim();
    }

    public static String resolveCorrelationId(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return newCorrelationId();
        }
        return normalizeOrThrow(incoming, CORRELATION_HEADER);
    }

    private static String randomSuffix() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(32);
        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }
        return builder.toString();
    }
}
