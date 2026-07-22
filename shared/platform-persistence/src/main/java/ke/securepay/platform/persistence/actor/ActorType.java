package ke.securepay.platform.persistence.actor;

/** Supported actor types for audit, idempotency, and outbox records. */
public enum ActorType {
    USER("user"),
    PARTNER_APPLICATION("partner_application"),
    SYSTEM("system"),
    ADMINISTRATOR("administrator"),
    COMPLIANCE_OFFICER("compliance_officer"),
    BANK_PROVIDER("bank_provider"),
    TEST("service");

    private final String apiValue;

    ActorType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
