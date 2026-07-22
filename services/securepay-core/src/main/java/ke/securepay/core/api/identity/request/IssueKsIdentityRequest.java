package ke.securepay.core.api.identity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ke.securepay.platform.identity.model.IdentityType;

public record IssueKsIdentityRequest(

        @NotBlank
        String issuanceRequestKey,

        @NotNull
        IdentityType identityType,

        @NotBlank
        String displayName
) {}
