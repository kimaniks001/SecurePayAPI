package ke.securepay.platform.identity.alias;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Normalizes and validates KS Number aliases for uniqueness and safety. */
public final class AliasNormalizer {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 32;

    private static final Pattern CANONICAL_KS_PATTERN = Pattern.compile("^ks[0-9]{3,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[a-z0-9._-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(".*@.*\\..+");
    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("^[a-z][a-z0-9+.-]*://", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_LIKE_PATTERN = Pattern.compile("^[+]?[0-9().\\s-]{7,}$");

    private static final Set<String> RESERVED_TERMS = Set.of(
            "admin",
            "administrator",
            "securepay",
            "support",
            "system",
            "root",
            "official",
            "helpdesk",
            "billing",
            "security",
            "api",
            "internal",
            "staff",
            "moderator");

    private AliasNormalizer() {}

    public record NormalizedAlias(String rawAlias, String normalizedAlias) {}

    public static NormalizedAlias normalizeOrThrow(String rawAlias) {
        if (rawAlias == null) {
            throw new InvalidAliasException("Alias must not be null");
        }
        String trimmed = rawAlias.trim();
        if (!trimmed.equals(rawAlias)) {
            throw new InvalidAliasException("Alias must not contain surrounding whitespace");
        }
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new InvalidAliasException("Alias length must be between " + MIN_LENGTH + " and " + MAX_LENGTH);
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidAliasException("Alias must not contain control characters");
        }
        if (CANONICAL_KS_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidAliasException("Alias must not impersonate a canonical KS Number");
        }
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidAliasException("Alias must not resemble an email address");
        }
        if (URL_SCHEME_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidAliasException("Alias must not contain a URL scheme");
        }
        if (PHONE_LIKE_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidAliasException("Alias must not resemble a phone number");
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CHARACTERS.matcher(normalized).matches()) {
            throw new InvalidAliasException("Alias contains unsupported characters");
        }
        if (RESERVED_TERMS.contains(normalized)) {
            throw new InvalidAliasException("Alias uses a reserved SecurePay term");
        }
        if (normalized.contains("admin") || normalized.contains("official")) {
            throw new InvalidAliasException("Alias uses a misleading administrative term");
        }
        return new NormalizedAlias(trimmed, normalized);
    }
}
