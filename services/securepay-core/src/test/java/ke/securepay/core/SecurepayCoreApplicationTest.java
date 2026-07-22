package ke.securepay.core;

import static org.assertj.core.api.Assertions.assertThat;

import ke.securepay.core.api.identity.controller.IdentityController;
import ke.securepay.core.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

class SecurepayCoreApplicationTest {

    @Test
    void onlyApprovedControllersAreExposed() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(
                new AnnotationTypeFilter(RestController.class)
        );

        var controllers = scanner
                .findCandidateComponents("ke.securepay.core")
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .toList();

        assertThat(controllers).containsExactlyInAnyOrder(
                HealthController.class.getName(),
                IdentityController.class.getName()
        );
    }
}
