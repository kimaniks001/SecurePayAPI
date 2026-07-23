package ke.securepay.core.api.identity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ke.securepay.platform.identity.model.IdentityStatus;

public record TransitionIdentityRequest(
        @NotNull
        IdentityStatus targetStatus,

        @NotBlank
        String reason
) {}
