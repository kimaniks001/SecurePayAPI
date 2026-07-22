plugins {
    `java-library`
}

dependencies {
    api(project(":shared:platform-common"))
    api(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}
