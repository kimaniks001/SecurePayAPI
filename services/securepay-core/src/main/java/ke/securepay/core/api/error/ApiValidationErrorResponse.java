package ke.securepay.core.api.error;

import java.util.Map;

public record ApiValidationErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors
) {}
