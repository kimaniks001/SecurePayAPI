plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

allprojects {
    group = "ke.securepay"
    version = property("securepay.version") as String

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
}

tasks.register("doctrineTest") {
    group = "verification"
    description = "Runs SecurePay doctrine compliance tests"
    dependsOn(":testing:doctrine:test")
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs all SecurePay integration tests"
    dependsOn(":services:securepay-core:integrationTest")
}
