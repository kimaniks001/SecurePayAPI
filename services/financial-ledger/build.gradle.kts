plugins {
    `java-library`
}

dependencies {
    api(project(":shared:platform-common"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}
