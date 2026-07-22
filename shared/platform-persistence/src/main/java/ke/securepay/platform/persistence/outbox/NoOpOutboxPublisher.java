package ke.securepay.platform.persistence.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpOutboxPublisher implements OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpOutboxPublisher.class);

    @Override
    public void publish(OutboxEventRecord event) {
        log.debug("outbox_publish_skipped eventId={} eventType={}", event.eventId(), event.eventType());
    }
}
