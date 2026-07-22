package ke.securepay.platform.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import ke.securepay.platform.common.errors.FoundationErrorCode;
import ke.securepay.platform.common.ids.IdentifierRules;
import ke.securepay.platform.common.ids.InvalidIdentifierException;
import ke.securepay.platform.observability.context.CorrelationContext;
import ke.securepay.platform.web.error.ErrorResponseWriter;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private final ErrorResponseWriter errorResponseWriter;

    public RequestCorrelationFilter(ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestId = resolveRequestId(request);
            String correlationId = resolveCorrelationId(request);

            CorrelationContext.setRequestId(requestId);
            CorrelationContext.setCorrelationId(correlationId);
            MDC.put(CorrelationContext.REQUEST_ID, requestId);
            MDC.put(CorrelationContext.CORRELATION_ID, correlationId);

            response.setHeader(IdentifierRules.REQUEST_HEADER, requestId);
            response.setHeader(IdentifierRules.CORRELATION_HEADER, correlationId);

            filterChain.doFilter(request, response);
        } catch (InvalidIdentifierException ex) {
            errorResponseWriter.write(
                    response,
                    HttpStatus.BAD_REQUEST.value(),
                    FoundationErrorCode.INVALID_CORRELATION_ID,
                    "The correlation identifier is invalid.");
        } finally {
            MDC.remove(CorrelationContext.REQUEST_ID);
            MDC.remove(CorrelationContext.CORRELATION_ID);
            CorrelationContext.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = firstHeader(request, IdentifierRules.REQUEST_HEADER, "X-Request-ID");
        if (incoming == null || incoming.isBlank()) {
            return IdentifierRules.newRequestId();
        }
        return IdentifierRules.normalizeOrThrow(incoming, IdentifierRules.REQUEST_HEADER);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String incoming = firstHeader(request, IdentifierRules.CORRELATION_HEADER, "X-Correlation-ID");
        try {
            return IdentifierRules.resolveCorrelationId(incoming);
        } catch (InvalidIdentifierException ex) {
            throw ex;
        }
    }

    private String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
