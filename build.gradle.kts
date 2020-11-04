import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0" apply false
    id("org.springframework.boot") version "3.0.0-SNAPSHOT" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
    kotlin("plugin.spring") version "1.6.20-RC" apply false
}

allprojects {
    group = "io.neos.fusion4j"
    version = "0.0.1-SNAPSHOT"

    ext {
        set("antlrVersion", "4.7.1")
        set("cucumberVersion", "7.0.0")
    }

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    tasks.withType<Test>().configureEach {
        useJUnit()
    }
}