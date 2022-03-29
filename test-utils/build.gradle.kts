plugins {
    `java-library`
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(platform(project(":platform")))

    api(project(":lang"))
    api(project(":runtime"))

    implementation(testLibs.cucumber.java8)
    implementation(testLibs.cucumber.junit)

    implementation(libs.jackson)
    implementation(libs.klogger)

    // TODO make runtime only and load class by string?
    implementation(libs.logback.core)
    implementation(libs.logback.classic)
}
