package ke.securepay.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // Uses Spring Boot defaults with explicit 404 handling via GlobalExceptionHandler.
}
