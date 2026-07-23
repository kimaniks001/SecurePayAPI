package ke.securepay.core.api.error;

import ke.securepay.platform.identity.exception.IdentityLifecycleException;
import ke.securepay.platform.identity.exception.IdentityNotFoundException;
import ke.securepay.platform.identity.exception.IssuanceOwnershipConflictException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiValidationErrorResponse handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return new ApiValidationErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                fieldErrors
        );
    }


    @ExceptionHandler(IdentityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleIdentityNotFound(
            IdentityNotFoundException exception) {

        return new ApiErrorResponse(
                "IDENTITY_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IdentityLifecycleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleIdentityLifecycleConflict(
            IdentityLifecycleException exception) {

        return new ApiErrorResponse(
                "IDENTITY_LIFECYCLE_CONFLICT",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IssuanceOwnershipConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleIssuanceOwnershipConflict(
            IssuanceOwnershipConflictException exception) {

        return new ApiErrorResponse(
                "ISSUANCE_OWNERSHIP_CONFLICT",
                exception.getMessage()
        );
    }
}
