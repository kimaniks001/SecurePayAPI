plugins {
    `java-library`
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.boot.dependencies.get().toString())
    }
}

dependencies {
    testImplementation(project(":shared:platform-common"))
    testImplementation(project(":services:securepay-core"))
    testImplementation(project(":services:financial-ledger"))
    testImplementation(project(":services:choice-bank-connector"))
    testImplementation(project(":services:evidence-service"))
    testImplementation(project(":services:notification-service"))
    testImplementation(project(":services:webhook-service"))
    testImplementation(project(":applications:control-centre"))
    testImplementation(project(":shared:platform-web"))
    testImplementation(project(":shared:platform-persistence"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.spring.web)
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootProject.projectDir
}
