package ke.securepay.platform.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import ke.securepay.platform.observability.logging.LogFields;
import ke.securepay.platform.observability.logging.SensitiveValueRedactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String route = SensitiveValueRedactor.safeRoute(request.getMethod(), request.getRequestURI());
        MDC.put(LogFields.HTTP_METHOD, request.getMethod());
        MDC.put(LogFields.HTTP_ROUTE, route);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startedAt;
            MDC.put(LogFields.DURATION_MS, String.valueOf(duration));
            MDC.put(LogFields.HTTP_STATUS, String.valueOf(response.getStatus()));
            log.info(
                    "http_request_completed method={} route={} status={} duration_ms={}",
                    request.getMethod(),
                    route,
                    response.getStatus(),
                    duration);
            MDC.remove(LogFields.HTTP_METHOD);
            MDC.remove(LogFields.HTTP_ROUTE);
            MDC.remove(LogFields.DURATION_MS);
            MDC.remove(LogFields.HTTP_STATUS);
        }
    }
}
