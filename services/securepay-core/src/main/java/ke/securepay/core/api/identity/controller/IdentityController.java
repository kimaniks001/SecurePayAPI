package ke.securepay.core.api.identity.controller;

import jakarta.validation.Valid;
import ke.securepay.core.api.identity.mapper.IdentityResponseMapper;
import ke.securepay.core.api.identity.request.IssueKsIdentityRequest;
import ke.securepay.core.api.identity.request.TransitionIdentityRequest;
import ke.securepay.core.api.identity.response.IssuedKsIdentityResponse;
import ke.securepay.core.api.identity.response.RetrievedIdentityResponse;
import ke.securepay.core.api.identity.response.TransitionedIdentityResponse;
import java.util.UUID;
import ke.securepay.platform.identity.alias.AliasNormalizer;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.command.LifecycleTransitionCommand;
import ke.securepay.platform.identity.exception.IdentityNotFoundException;
import ke.securepay.platform.identity.ksnumber.KsNumber;
import ke.securepay.platform.identity.model.KsIdentityRecord;
import ke.securepay.platform.identity.result.IssuedKsIdentityResult;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import ke.securepay.platform.identity.service.KsIdentityLifecycleService;
import ke.securepay.platform.identity.service.KsIdentityQueryService;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identities")
public class IdentityController {

    private final KsIdentityIssuanceService issuanceService;
    private final KsIdentityLifecycleService lifecycleService;
    private final KsIdentityQueryService queryService;

    public IdentityController(
            KsIdentityIssuanceService issuanceService,
            KsIdentityLifecycleService lifecycleService,
            KsIdentityQueryService queryService) {
        this.issuanceService = issuanceService;
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
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


    @GetMapping("/by-ksnumber/{canonicalKsNumber}")
    public RetrievedIdentityResponse retrieveByCanonicalKsNumber(
            @PathVariable String canonicalKsNumber) {

        String validatedKsNumber =
                KsNumber.parse(canonicalKsNumber).canonicalValue();

        KsIdentityRecord record =
                queryService.findByCanonicalKsNumber(validatedKsNumber)
                        .orElseThrow(() ->
                                new IdentityNotFoundException(
                                        "Identity not found"));

        return IdentityResponseMapper.retrievedFrom(record);
    }


    @GetMapping("/by-alias/{alias}")
    public RetrievedIdentityResponse retrieveByAlias(
            @PathVariable String alias) {

        String normalizedAlias =
                AliasNormalizer.normalizeOrThrow(alias).normalizedAlias();

        KsIdentityRecord record =
                queryService.findByNormalizedAlias(normalizedAlias)
                        .orElseThrow(() ->
                                new IdentityNotFoundException(
                                        "Identity not found"));

        return IdentityResponseMapper.retrievedFrom(record);
    }


    @GetMapping("/{identityId}")
    public RetrievedIdentityResponse retrieveById(
            @PathVariable UUID identityId) {

        KsIdentityRecord record = queryService.findById(identityId)
                .orElseThrow(() ->
                        new IdentityNotFoundException("Identity not found"));

        return IdentityResponseMapper.retrievedFrom(record);
    }

    @PatchMapping("/{identityId}/status")
    public TransitionedIdentityResponse transitionStatus(
            @PathVariable UUID identityId,
            @Valid @RequestBody TransitionIdentityRequest request) {

        LifecycleTransitionCommand command = new LifecycleTransitionCommand(
                identityId,
                request.targetStatus(),
                request.reason(),
                ActorContextFactory.system("securepay-core")
        );

        KsIdentityRecord record = lifecycleService.transition(command);

        return IdentityResponseMapper.from(record);
    }
}
