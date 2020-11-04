plugins {
    `java-library`
    id("kotlin")
}

dependencies {
    api(platform(project(":platform")))
    api(project(":lang"))
    api(project(":runtime"))

    implementation("io.cucumber", "cucumber-java8", project.ext.get("cucumberVersion") as String)
    implementation("io.cucumber", "cucumber-junit", project.ext.get("cucumberVersion") as String)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")

    implementation("io.github.microutils:kotlin-logging-jvm")
    runtimeOnly("ch.qos.logback:logback-core")
    runtimeOnly("ch.qos.logback:logback-classic")
}
