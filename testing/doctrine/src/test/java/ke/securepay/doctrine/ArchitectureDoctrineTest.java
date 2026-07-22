package ke.securepay.doctrine;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

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

    @Test
    void auditRepositoryHasNoUpdateOrDeleteMethods() {
        ArchRule rule = noMethods()
                .that()
                .areDeclaredInClassesThat()
                .haveSimpleName("AuditEventRepository")
                .should()
                .haveNameMatching("update.*|delete.*|remove.*");
        rule.check(classes);
    }

    @Test
    void transactionalOutboxPersistenceExists() {
        ArchRule rule = classes()
                .that()
                .haveSimpleName("OutboxService")
                .should()
                .resideInAPackage("ke.securepay.platform.persistence.outbox..");
        rule.check(classes);
    }

    @Test
    void ksNumberFormatterDoesNotDependOnWebOrPersistence() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("ke.securepay.platform.identity.ksnumber..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web..", "org.springframework.jdbc..");
        rule.check(classes);
    }

    @Test
    void identityDomainDoesNotDependOnFinancialLedger() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("ke.securepay.platform.identity..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("ke.securepay.ledger..");
        rule.check(classes);
    }

    @Test
    void identityRepositoriesHaveNoDeleteMethods() {
        ArchRule rule = noMethods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage("ke.securepay.platform.identity.persistence..")
                .should()
                .haveNameMatching("delete.*|remove.*");
        rule.check(classes);
    }

    @Test
    void identityIssuanceServiceExistsInIdentityDomain() {
        ArchRule rule = classes()
                .that()
                .haveSimpleName("KsIdentityIssuanceService")
                .should()
                .resideInAPackage("ke.securepay.platform.identity.service..");
        rule.check(classes);
    }

    @Test
    void identityEventsUseStableExternalNames() {
        ArchRule rule = classes()
                .that()
                .haveSimpleName("IdentityEventTypes")
                .should()
                .resideInAPackage("ke.securepay.platform.identity.events..");
        rule.check(classes);
    }

    @Test
    void noAuthenticationDomainClassesExist() {
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage("ke.securepay..")
                .should()
                .haveSimpleNameContaining("UserAccount")
                .orShould()
                .haveSimpleNameContaining("LoginController")
                .orShould()
                .haveSimpleNameContaining("OtpService");
        rule.check(classes);
    }

    @Test
    void noSecureLinkDomainClassesExist() {
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage("ke.securepay..")
                .should()
                .haveSimpleNameContaining("SecureLink");
        rule.check(classes);
    }

    @Test
    void noLedgerBusinessClassesExist() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("ke.securepay.ledger..")
                .should()
                .haveNameMatching("Journal.*|LedgerAccount.*|Posting.*");
        rule.check(classes);
    }

    @Test
    void noPaymentDomainClassesExist() {
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage("ke.securepay..")
                .should()
                .haveSimpleNameContaining("PaymentIntent")
                .orShould()
                .haveSimpleNameContaining("SettlementService");
        rule.check(classes);
    }

    @Test
    void auditWriterHasNoUpdateOrDeleteMethods() {
        ArchRule rule = noMethods()
                .that()
                .areDeclaredInClassesThat()
                .haveSimpleName("AuditWriter")
                .should()
                .haveNameMatching("update.*|delete.*|remove.*");
        rule.check(classes);
    }

    @Test
    void idempotencyPersistenceExists() {
        ArchRule rule = classes()
                .that()
                .haveSimpleName("IdempotencyRepository")
                .should()
                .resideInAPackage("ke.securepay.platform.persistence.idempotency..");
        rule.check(classes);
    }

    @Test
    void publicEventContractsDoNotExposeJavaPackageNames() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("ke.securepay.platform.persistence.events..")
                .should()
                .haveSimpleNameContaining("Securepay")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void noEnvironmentVariableDisablesPersistenceDoctrine() {
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage("ke.securepay.platform.persistence..", "ke.securepay.core..")
                .should()
                .haveSimpleNameContaining("DisableAudit")
                .orShould()
                .haveSimpleNameContaining("DisableOutbox")
                .orShould()
                .haveSimpleNameContaining("DisableIdempotency");
        rule.check(classes);
    }
}
