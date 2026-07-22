package ke.securepay.platform.persistence.outbox;

/** No-op publisher for Phase 3 — events are persisted only. */
public interface OutboxPublisher {
    void publish(OutboxEventRecord event);
}
