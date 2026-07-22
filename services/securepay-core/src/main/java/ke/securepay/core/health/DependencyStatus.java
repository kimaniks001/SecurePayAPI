package ke.securepay.core.health;

public enum DependencyStatus {
    HEALTHY("healthy"),
    DEGRADED("degraded"),
    UNAVAILABLE("unavailable");

    private final String apiValue;

    DependencyStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
