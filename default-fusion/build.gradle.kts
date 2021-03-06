description = "fusion4j - Default Fusion Prototypes / Implementations / EEL Helpers - Neos.Fusion"

plugins {
    `java-library`
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(platform(project(":platform")))
    api(project(":lang"))
    api(project(":runtime"))

    implementation(libs.klogger)
    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    implementation(libs.apache.text)

    testImplementation(platform(project(":test-utils")))
}

sourceSets {
    main {
        resources {
            srcDir("src/main/fusion")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

configure<PublishingExtension> {
    publications {
        withType<MavenPublication> {
            from(components["java"])
        }
    }
}