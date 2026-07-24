package ke.securepay.platform.persistence.actor;

import ke.securepay.platform.common.ids.IdentifierRules;
import ke.securepay.platform.observability.context.CorrelationContext;

/** Creates trusted actor contexts for Phase 3 technical flows. */
public final class ActorContextFactory {

    private static final String SYSTEM_ACTOR_ID = "system";
    private static final String TEST_ACTOR_ID = "test-actor";

    private ActorContextFactory() {}

    public static ActorContext user(
            String actorId,
            String actorKsNumber,
            String authenticationMethod,
            String sourceService,
            String applicationId,
            String sourceIpHash,
            String deviceId) {
        return new ActorContext(
                ActorType.USER,
                actorId,
                actorKsNumber,
                applicationId,
                true,
                authenticationMethod,
                resolveRequestId(),
                resolveCorrelationId(),
                sourceService,
                sourceIpHash,
                deviceId);
    }

    public static ActorContext system(String sourceService) {
        return new ActorContext(
                ActorType.SYSTEM,
                SYSTEM_ACTOR_ID,
                null,
                null,
                false,
                null,
                resolveRequestId(),
                resolveCorrelationId(),
                sourceService,
                null,
                null);
    }

    public static ActorContext test(String sourceService) {
        return new ActorContext(
                ActorType.TEST,
                TEST_ACTOR_ID,
                null,
                null,
                false,
                null,
                resolveRequestId(),
                resolveCorrelationId(),
                sourceService,
                null,
                null);
    }

    public static ActorContext withCorrelation(ActorContext base, String requestId, String correlationId) {
        return new ActorContext(
                base.actorType(),
                base.actorId(),
                base.actorKsNumber(),
                base.applicationId(),
                base.authenticated(),
                base.authenticationMethod(),
                requestId,
                correlationId,
                base.sourceService(),
                base.sourceIpHash(),
                base.deviceId());
    }

    private static String resolveRequestId() {
        String requestId = CorrelationContext.requestId();
        return requestId != null ? requestId : IdentifierRules.newRequestId();
    }

    private static String resolveCorrelationId() {
        String correlationId = CorrelationContext.correlationId();
        return correlationId != null ? correlationId : IdentifierRules.newCorrelationId();
    }
}
