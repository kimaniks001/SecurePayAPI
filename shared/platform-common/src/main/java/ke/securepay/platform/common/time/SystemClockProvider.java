package ke.securepay.platform.common.time;

import java.time.Clock;

/** Production {@link ClockProvider} using the JVM system clock in UTC. */
public final class SystemClockProvider implements ClockProvider {

    private final Clock clock;

    public SystemClockProvider() {
        this(Clock.systemUTC());
    }

    SystemClockProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Clock clock() {
        return clock;
    }
}
