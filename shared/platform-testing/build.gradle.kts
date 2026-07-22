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
    api(project(":shared:platform-common"))
    api(libs.spring.boot.starter.test)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.postgresql)
    api(libs.testcontainers.redis)
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    implementation(libs.json.schema.validator)
}
