package ke.securepay.platform.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import ke.securepay.platform.common.errors.FoundationErrorCode;
import ke.securepay.platform.web.api.ErrorBody;
import ke.securepay.platform.web.api.ErrorEnvelope;
import ke.securepay.platform.web.api.ResponseMeta;
import ke.securepay.platform.web.api.ResponseMetaFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ErrorResponseWriter(ObjectMapper objectMapper, ResponseMetaFactory responseMetaFactory) {
        this.objectMapper = objectMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    public void write(
            HttpServletResponse response,
            int status,
            FoundationErrorCode code,
            String message)
            throws IOException {
        ResponseMeta meta = responseMetaFactory.current();
        ErrorEnvelope envelope = ErrorEnvelope.of(new ErrorBody(code.name(), message, null), meta);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), envelope);
    }
}
