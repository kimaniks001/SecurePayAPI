package ke.securepay.core.api.error;

public record ApiErrorResponse(
        String code,
        String message
) {}
