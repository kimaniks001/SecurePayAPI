package ke.securepay.platform.persistence.actor;

import java.util.Objects;

/** Trusted technical actor and request context propagated into persistence records. */
public record ActorContext(
        ActorType actorType,
        String actorId,
        String actorKsNumber,
        String applicationId,
        boolean authenticated,
        String authenticationMethod,
        String requestId,
        String correlationId,
        String sourceService,
        String sourceIpHash,
        String deviceId) {

    public ActorContext {
        Objects.requireNonNull(actorType, "actorType");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(sourceService, "sourceService");
    }
}
