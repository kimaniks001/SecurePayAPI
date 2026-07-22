package ke.securepay.platform.observability.logging;

import java.util.Locale;
import java.util.regex.Pattern;

/** Redacts sensitive values before they are written to logs. */
public final class SensitiveValueRedactor {

    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[\\w\\-._~+/]+=*");
    private static final Pattern PASSWORD = Pattern.compile("(?i)(password|passwd|secret|token|otp)\\s*[:=]\\s*\\S+");

    private SensitiveValueRedactor() {}

    public static String redact(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String redacted = BEARER.matcher(input).replaceAll("Bearer [REDACTED]");
        redacted = PASSWORD.matcher(redacted).replaceAll("$1=[REDACTED]");
        return redacted;
    }

    public static String safeRoute(String method, String path) {
        if (path == null) {
            return "unknown";
        }
        return (method == null ? "UNKNOWN" : method.toUpperCase(Locale.ROOT)) + " " + path;
    }
}
