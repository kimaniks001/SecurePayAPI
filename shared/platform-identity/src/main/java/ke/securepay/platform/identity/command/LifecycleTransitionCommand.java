package ke.securepay.platform.identity.command;

import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.persistence.actor.ActorContext;
import java.util.UUID;

public record LifecycleTransitionCommand(
        UUID identityId,
        IdentityStatus targetStatus,
        String reason,
        ActorContext actorContext) {}
