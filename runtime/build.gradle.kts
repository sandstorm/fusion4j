plugins {
    `java-library`
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(platform(project(":platform")))
    api(project(":lang"))

    implementation(libs.apache.jexl)

    implementation(libs.klogger)

    // testing
    testImplementation(platform(project(":test-utils")))
    testImplementation(testLibs.cucumber.java8)
    testImplementation(testLibs.cucumber.junit)
}