plugins {
    `java-library`
    id("kotlin")

    antlr
}

dependencies {
    antlr("org.antlr", "antlr4", project.ext.get("antlrVersion") as String)

    api(platform(project(":platform")))

    api("org.antlr", "antlr4-runtime")
    implementation(kotlin("script-runtime"))

    implementation("org.reflections:reflections")
    implementation("io.github.microutils:kotlin-logging-jvm")
    runtimeOnly("ch.qos.logback:logback-core")
    runtimeOnly("ch.qos.logback:logback-classic")

    testImplementation("io.cucumber", "cucumber-java8", project.ext.get("cucumberVersion") as String)
    testImplementation("io.cucumber", "cucumber-junit", project.ext.get("cucumberVersion") as String)
    testImplementation(platform(project(":test-utils")))
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
