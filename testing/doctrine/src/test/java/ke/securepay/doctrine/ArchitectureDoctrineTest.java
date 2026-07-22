package ke.securepay.doctrine;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureDoctrineTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(
                        "ke.securepay.core",
                        "ke.securepay.ledger",
                        "ke.securepay.choice",
                        "ke.securepay.evidence",
                        "ke.securepay.notification",
                        "ke.securepay.webhook",
                        "ke.securepay.controlcentre",
                        "ke.securepay.platform");
    }

    @Test
    void financialLedgerMustNotDependOnSecurepayCore() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("ke.securepay.ledger..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("ke.securepay.core..");
        rule.check(classes);
    }

    @Test
    void sharedModulesMustNotDependOnServices() {
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage("ke.securepay.platform..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "ke.securepay.core..",
                        "ke.securepay.ledger..",
                        "ke.securepay.choice..",
                        "ke.securepay.evidence..",
                        "ke.securepay.notification..",
                        "ke.securepay.webhook..");
        rule.check(classes);
    }

    @Test
    void noChoiceHttpClientExists() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("ke.securepay.choice..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.web.client.RestTemplate");
        rule.check(classes);
    }

    @Test
    void noPaymentReadyOverrideExists() {
        ArchRule rule = noClasses()
                .that()
                .haveSimpleNameContaining("PaymentReady")
                .should()
                .haveSimpleNameContaining("Override")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void onlyHealthControllerExposesHttpEndpointsInCore() {
        ArchRule rule = classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .and()
                .resideInAPackage("ke.securepay.core..")
                .should()
                .haveSimpleName("HealthController");
        rule.check(classes);
    }

    @Test
    void choiceConnectorContainsOnlySkeletonMarker() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("ke.securepay.choice..")
                .should()
                .haveSimpleName("ChoiceConnectorModule");
        rule.check(classes);
    }
}
