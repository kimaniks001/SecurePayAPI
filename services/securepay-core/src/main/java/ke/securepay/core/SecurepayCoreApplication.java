package ke.securepay.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "ke.securepay.core")
@ConfigurationPropertiesScan(basePackages = "ke.securepay.core.config")
public class SecurepayCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurepayCoreApplication.class, args);
    }
}
