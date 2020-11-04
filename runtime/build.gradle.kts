plugins {
    `java-library`
    id("kotlin")
}

dependencies {
    api(platform(project(":platform")))
    api(project(":lang"))

    api("io.projectreactor", "reactor-core")

    testImplementation("io.cucumber", "cucumber-java8", project.ext.get("cucumberVersion") as String)
    testImplementation("io.cucumber", "cucumber-junit", project.ext.get("cucumberVersion") as String)
    testImplementation(platform(project(":test-utils")))

    implementation("io.github.microutils:kotlin-logging-jvm")
    runtimeOnly("ch.qos.logback:logback-core")
    runtimeOnly("ch.qos.logback:logback-classic")

    implementation("org.apache.commons:commons-jexl3")
}
