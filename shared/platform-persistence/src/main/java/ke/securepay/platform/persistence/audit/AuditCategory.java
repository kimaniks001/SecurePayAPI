package ke.securepay.platform.persistence.audit;

/** Platform and domain audit categories. */
public enum AuditCategory {
    PLATFORM_TECHNICAL("platform.technical"),
    IDENTITY("identity");

    private final String value;

    AuditCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
