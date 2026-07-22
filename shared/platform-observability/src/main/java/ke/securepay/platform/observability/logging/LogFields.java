package ke.securepay.platform.observability.logging;

/** Structured log field names used across SecurePay services. */
public final class LogFields {

    public static final String TIMESTAMP = "@timestamp";
    public static final String LEVEL = "level";
    public static final String SERVICE = "service";
    public static final String ENVIRONMENT = "environment";
    public static final String REQUEST_ID = "request_id";
    public static final String CORRELATION_ID = "correlation_id";
    public static final String HTTP_METHOD = "http_method";
    public static final String HTTP_ROUTE = "http_route";
    public static final String HTTP_STATUS = "http_status";
    public static final String DURATION_MS = "duration_ms";

    private LogFields() {}
}
