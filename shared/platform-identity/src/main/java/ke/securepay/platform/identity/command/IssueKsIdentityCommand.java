package ke.securepay.platform.identity.command;

import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.persistence.actor.ActorContext;

public record IssueKsIdentityCommand(
        String issuanceRequestKey,
        IdentityType identityType,
        String displayName,
        ActorContext actorContext) {}
