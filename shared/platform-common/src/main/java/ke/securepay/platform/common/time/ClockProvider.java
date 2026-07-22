package ke.securepay.platform.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Abstraction over system time for testability. */
public interface ClockProvider {

    Clock clock();

    default Instant now() {
        return clock().instant();
    }

    default ClockProvider utc() {
        return () -> clock().withZone(ZoneOffset.UTC);
    }
}
