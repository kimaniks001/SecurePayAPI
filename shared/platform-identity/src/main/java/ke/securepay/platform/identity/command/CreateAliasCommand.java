package ke.securepay.platform.identity.command;

import ke.securepay.platform.identity.model.AliasType;
import ke.securepay.platform.persistence.actor.ActorContext;
import java.util.UUID;

public record CreateAliasCommand(
        UUID identityId,
        String alias,
        AliasType aliasType,
        boolean primaryDisplayAlias,
        ActorContext actorContext) {}
