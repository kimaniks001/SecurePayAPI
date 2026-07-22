plugins {
    `java-library`
}

dependencies {
    api(libs.slf4j.api)
    api(libs.jakarta.validation.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}
