package ke.securepay.platform.identity.service;

import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.result.IssuedKsIdentityResult;

public interface KsIdentityIssuanceService {

    IssuedKsIdentityResult issue(IssueKsIdentityCommand command);
}
