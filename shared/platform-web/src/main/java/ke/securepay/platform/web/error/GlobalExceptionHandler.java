package ke.securepay.platform.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import ke.securepay.platform.common.errors.FoundationErrorCode;
import ke.securepay.platform.common.ids.InvalidIdentifierException;
import ke.securepay.platform.web.api.ErrorBody;
import ke.securepay.platform.web.api.ErrorEnvelope;
import ke.securepay.platform.web.api.ResponseMeta;
import ke.securepay.platform.web.api.ResponseMetaFactory;
import ke.securepay.platform.web.api.ValidationErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ResponseMetaFactory responseMetaFactory;

    public GlobalExceptionHandler(ResponseMetaFactory responseMetaFactory) {
        this.responseMetaFactory = responseMetaFactory;
    }

    @ExceptionHandler(InvalidIdentifierException.class)
    public ResponseEntity<ErrorEnvelope> handleInvalidIdentifier(InvalidIdentifierException ex) {
        return error(
                HttpStatus.BAD_REQUEST,
                FoundationErrorCode.INVALID_CORRELATION_ID,
                "The correlation identifier is invalid.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        List<ValidationErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return error(
                HttpStatus.BAD_REQUEST,
                FoundationErrorCode.VALIDATION_ERROR,
                "One or more fields failed validation.",
                details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorEnvelope> handleMalformedJson(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, FoundationErrorCode.MALFORMED_JSON, "The request body is not valid JSON.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorEnvelope> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return error(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                FoundationErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "The request media type is not supported.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorEnvelope> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return error(
                HttpStatus.METHOD_NOT_ALLOWED,
                FoundationErrorCode.METHOD_NOT_ALLOWED,
                "The HTTP method is not allowed for this endpoint.");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorEnvelope> handleNotFound(Exception ex) {
        return error(HttpStatus.NOT_FOUND, FoundationErrorCode.NOT_FOUND, "The requested resource was not found.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(Exception ex, HttpServletRequest request) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                FoundationErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred.");
    }

    private ValidationErrorDetail toDetail(FieldError fieldError) {
        return new ValidationErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage() == null ? "invalid value" : fieldError.getDefaultMessage(),
                "invalid_value");
    }

    private ResponseEntity<ErrorEnvelope> error(HttpStatus status, FoundationErrorCode code, String message) {
        return error(status, code, message, null);
    }

    private ResponseEntity<ErrorEnvelope> error(
            HttpStatus status, FoundationErrorCode code, String message, List<ValidationErrorDetail> details) {
        ResponseMeta meta = responseMetaFactory.current();
        ErrorBody body = new ErrorBody(code.name(), message, details);
        return ResponseEntity.status(status).body(ErrorEnvelope.of(body, meta));
    }
}
