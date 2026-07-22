package ke.securepay.platform.identity.ksnumber;

import java.util.Objects;

/** Immutable canonical KS Number value object. */
public final class KsNumber {

    private final String canonicalValue;
    private final long sequenceNumber;

    KsNumber(String canonicalValue, long sequenceNumber) {
        this.canonicalValue = canonicalValue;
        this.sequenceNumber = sequenceNumber;
    }

    public static KsNumber fromSequence(long sequenceNumber) {
        return new KsNumber(KsNumberFormatter.format(sequenceNumber), sequenceNumber);
    }

    public static KsNumber parse(String rawValue) {
        return KsNumberParser.parse(rawValue);
    }

    public String canonicalValue() {
        return canonicalValue;
    }

    public long sequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof KsNumber that)) {
            return false;
        }
        return sequenceNumber == that.sequenceNumber && canonicalValue.equals(that.canonicalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonicalValue, sequenceNumber);
    }

    @Override
    public String toString() {
        return canonicalValue;
    }
}
