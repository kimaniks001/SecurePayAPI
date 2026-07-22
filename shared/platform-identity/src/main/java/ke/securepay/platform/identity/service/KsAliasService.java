package ke.securepay.platform.identity.service;

import ke.securepay.platform.identity.command.AliasLifecycleTransitionCommand;
import ke.securepay.platform.identity.command.CreateAliasCommand;
import ke.securepay.platform.identity.model.KsNumberAliasRecord;

public interface KsAliasService {

    KsNumberAliasRecord createAlias(CreateAliasCommand command);

    KsNumberAliasRecord transitionAlias(AliasLifecycleTransitionCommand command);
}
