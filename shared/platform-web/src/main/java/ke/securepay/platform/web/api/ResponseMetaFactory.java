package ke.securepay.platform.web.api;

import ke.securepay.platform.common.ApiVersion;
import ke.securepay.platform.common.time.ClockProvider;
import ke.securepay.platform.observability.context.CorrelationContext;
import org.springframework.stereotype.Component;

@Component
public class ResponseMetaFactory {

    private final ClockProvider clockProvider;

    public ResponseMetaFactory(ClockProvider clockProvider) {
        this.clockProvider = clockProvider;
    }

    public ResponseMeta current() {
        return ResponseMeta.of(
                CorrelationContext.requestId(),
                CorrelationContext.correlationId(),
                ApiVersion.CURRENT,
                clockProvider.now());
    }
}
