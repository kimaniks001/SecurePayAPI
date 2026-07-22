package ke.securepay.platform.identity.service;

import ke.securepay.platform.identity.command.LifecycleTransitionCommand;
import ke.securepay.platform.identity.model.KsIdentityRecord;

public interface KsIdentityLifecycleService {

    KsIdentityRecord transition(LifecycleTransitionCommand command);
}
