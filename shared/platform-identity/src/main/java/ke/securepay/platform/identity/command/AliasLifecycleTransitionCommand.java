package ke.securepay.platform.identity.command;

import ke.securepay.platform.identity.model.AliasStatus;
import ke.securepay.platform.persistence.actor.ActorContext;
import java.util.UUID;

public record AliasLifecycleTransitionCommand(
        UUID aliasId,
        AliasStatus targetStatus,
        String reason,
        ActorContext actorContext) {}
