package ke.securepay.platform.observability.metrics;

public final class PersistenceMetricNames {

    public static final String AUDIT_EVENTS_APPENDED = "securepay.audit.events.appended";
    public static final String IDEMPOTENCY_CREATED = "securepay.idempotency.created";
    public static final String IDEMPOTENCY_REPLAYED = "securepay.idempotency.replayed";
    public static final String IDEMPOTENCY_CONFLICTS = "securepay.idempotency.conflicts";
    public static final String OUTBOX_PENDING = "securepay.outbox.pending";
    public static final String OUTBOX_PUBLISHED = "securepay.outbox.published";
    public static final String OUTBOX_FAILED = "securepay.outbox.failed";
    public static final String OUTBOX_DEAD_LETTER = "securepay.outbox.dead_letter";

    private PersistenceMetricNames() {}
}
