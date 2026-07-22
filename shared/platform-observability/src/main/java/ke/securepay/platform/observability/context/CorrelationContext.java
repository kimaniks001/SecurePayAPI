package ke.securepay.platform.observability.context;

/** Request-scoped correlation values propagated through logs and responses. */
public final class CorrelationContext {

    public static final String REQUEST_ID = "request_id";
    public static final String CORRELATION_ID = "correlation_id";
    public static final String SERVICE = "service";
    public static final String ENVIRONMENT = "environment";

    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> CORRELATION_ID_HOLDER = new ThreadLocal<>();

    private CorrelationContext() {}

    public static void setRequestId(String requestId) {
        REQUEST_ID_HOLDER.set(requestId);
    }

    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID_HOLDER.set(correlationId);
    }

    public static String requestId() {
        return REQUEST_ID_HOLDER.get();
    }

    public static String correlationId() {
        return CORRELATION_ID_HOLDER.get();
    }

    public static void clear() {
        REQUEST_ID_HOLDER.remove();
        CORRELATION_ID_HOLDER.remove();
    }
}
