package ke.securepay.platform.identity.ksnumber;

import java.util.regex.Pattern;

/** Strict parser for canonical KS Number strings. */
public final class KsNumberParser {

    private static final Pattern CANONICAL_PATTERN = Pattern.compile("^KS[0-9]{3,}$");

    private KsNumberParser() {}

    public static KsNumber parse(String rawValue) {
        if (rawValue == null) {
            throw new InvalidKsNumberException("KS Number must not be null");
        }
        String normalized = rawValue.trim();
        if (!normalized.equals(rawValue)) {
            throw new InvalidKsNumberException("KS Number must not contain surrounding whitespace");
        }
        if (!CANONICAL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidKsNumberException("Malformed canonical KS Number: " + rawValue);
        }
        long sequence = Long.parseLong(normalized.substring(2));
        if (sequence <= 0) {
            throw new InvalidKsNumberException("KS Number sequence must be positive");
        }
        return new KsNumber(normalized, sequence);
    }
}
