package ke.securepay.platform.observability.metrics;

/** Metric naming conventions for Micrometer-compatible instrumentation. */
public final class MetricNames {

    public static final String HTTP_SERVER_REQUESTS = "securepay.http.server.requests";
    public static final String DEPENDENCY_HEALTH = "securepay.dependency.health";

    private MetricNames() {}

    public static String dependencyGauge(String dependencyName) {
        return DEPENDENCY_HEALTH + "." + dependencyName;
    }
}
