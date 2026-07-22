package ke.securepay.platform.persistence.audit;

/** Phase 3 technical audit categories only. */
public enum AuditCategory {
    PLATFORM_TECHNICAL("platform.technical");

    private final String value;

    AuditCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
