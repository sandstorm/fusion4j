description = "fusion4j - Language module - Parser / Semantic"

plugins {
    `java-library`
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.jvm)
    antlr
}

dependencies {
    antlr(libs.antlr)
    api(platform(project(":platform")))

    implementation(libs.antlr.runtime)

    implementation(kotlin("script-runtime"))

    implementation(libs.reflections)
    implementation(libs.klogger)

    // testing
    testImplementation(platform(project(":test-utils")))
    testImplementation(testLibs.cucumber.java8)
    testImplementation(testLibs.cucumber.junit)
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets.main {
    java.srcDirs(java.srcDirs + "build/generated-src/antlr/main")
}

tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-package", "io.neos.fusion4j.lang.antlr")
    outputDirectory = File("build/generated-src/antlr/main/io/neos/fusion4j/lang/antlr")
}

configure<PublishingExtension> {
    publications {
        withType<MavenPublication> {
            from(components["java"])
        }
    }
}