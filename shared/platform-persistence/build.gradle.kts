plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.boot.dependencies.get().toString())
    }
}

dependencies {
    api(project(":shared:platform-common"))
    api(project(":shared:platform-observability"))

    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.context)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.jakarta.validation.api)
    implementation(libs.slf4j.api)
    implementation(libs.micrometer.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.json.schema.validator)
}

tasks.test {
    workingDir = rootProject.projectDir
}
