plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.boot.dependencies.get().toString())
    }
}

sourceSets {
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations {
    named("integrationTestImplementation") {
        extendsFrom(configurations.getByName("testImplementation"))
    }
    named("integrationTestRuntimeOnly") {
        extendsFrom(configurations.getByName("testRuntimeOnly"))
    }
}

dependencies {
    implementation(project(":shared:platform-common"))
    implementation(project(":shared:platform-observability"))
    implementation(project(":shared:platform-web"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgresql)
    implementation(libs.logstash.logback.encoder)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(project(":shared:platform-testing"))
    testImplementation(libs.spring.boot.starter.test)

    add("integrationTestImplementation", project(":shared:platform-testing"))
    add("integrationTestImplementation", libs.spring.boot.starter.test)
    add("integrationTestImplementation", libs.spring.boot.testcontainers)
    add("integrationTestImplementation", libs.testcontainers.junit.jupiter)
    add("integrationTestImplementation", libs.testcontainers.postgresql)
    add("integrationTestImplementation", libs.testcontainers.redis)
}

springBoot {
    buildInfo()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("securepay-core.jar")
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("plain")
}

tasks.named<ProcessResources>("processResources") {
    from("${rootProject.projectDir}/database/migrations") {
        include("V*.sql")
        into("db/migration")
    }
}

tasks.named<Test>("test") {
    description = "Runs unit tests (no Testcontainers)."
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs Testcontainers-backed integration tests (Docker required when SECUREPAY_REQUIRE_TESTCONTAINERS=true)."
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    useJUnitPlatform()
}
