package ke.securepay.platform.identity.metrics;

public final class IdentityMetricNames {

    public static final String IDENTITY_ISSUED = "securepay.identity.issued";
    public static final String IDENTITY_ISSUANCE_REPLAYED = "securepay.identity.issuance.replayed";
    public static final String IDENTITY_ISSUANCE_CONFLICT = "securepay.identity.issuance.conflict";
    public static final String IDENTITY_STATUS_CHANGED = "securepay.identity.status.changed";
    public static final String IDENTITY_ALIAS_CREATED = "securepay.identity.alias.created";
    public static final String IDENTITY_ALIAS_CONFLICT = "securepay.identity.alias.conflict";

    private IdentityMetricNames() {}
}
