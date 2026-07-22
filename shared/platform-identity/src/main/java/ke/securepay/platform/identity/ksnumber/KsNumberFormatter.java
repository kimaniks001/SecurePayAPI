package ke.securepay.platform.identity.ksnumber;

/** Formats canonical sequence numbers into immutable KS Number strings. */
public final class KsNumberFormatter {

    private static final String PREFIX = "KS";
    private static final int MINIMUM_WIDTH = 3;

    private KsNumberFormatter() {}

    public static String format(long sequenceNumber) {
        if (sequenceNumber <= 0) {
            throw new InvalidKsNumberException("Sequence number must be positive: " + sequenceNumber);
        }
        String digits = Long.toString(sequenceNumber);
        if (digits.length() < MINIMUM_WIDTH) {
            digits = "0".repeat(MINIMUM_WIDTH - digits.length()) + digits;
        }
        return PREFIX + digits;
    }
}
