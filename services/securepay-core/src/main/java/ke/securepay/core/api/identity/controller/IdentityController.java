package ke.securepay.core.api.identity.controller;

import jakarta.validation.Valid;
import ke.securepay.core.api.identity.mapper.IdentityResponseMapper;
import ke.securepay.core.api.identity.request.IssueKsIdentityRequest;
import ke.securepay.core.api.identity.response.IssuedKsIdentityResponse;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.result.IssuedKsIdentityResult;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identities")
public class IdentityController {

    private final KsIdentityIssuanceService issuanceService;

    public IdentityController(KsIdentityIssuanceService issuanceService) {
        this.issuanceService = issuanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedKsIdentityResponse issue(
            @Valid @RequestBody IssueKsIdentityRequest request) {

        IssueKsIdentityCommand command = new IssueKsIdentityCommand(
                request.issuanceRequestKey(),
                request.identityType(),
                request.displayName(),
                ActorContextFactory.system("securepay-core")
        );

        IssuedKsIdentityResult result = issuanceService.issue(command);

        return IdentityResponseMapper.from(result);
    }
}
