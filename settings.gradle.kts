rootProject.name = "fusion4j"

include("default-fusion")
include("platform")
include("lang")
include("runtime")
include("styleguide")
include("test-utils")

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }

    versionCatalogs {
        create("libs") {
            version("antlr", "4.9.3")
            version("logback", "1.2.10")
            version("kotlin", "1.6.20-RC2")
            version("spring", "3.0.0-SNAPSHOT")

            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin-spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")

            plugin("spring-boot", "org.springframework.boot").versionRef("spring")
            plugin("spring-dependencyManagement", "io.spring.dependency-management").version("1.0.11.RELEASE")

            library("jackson", "com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
            library("antlr", "org.antlr", "antlr4").versionRef("antlr")
            library("antlr-runtime", "org.antlr", "antlr4-runtime").versionRef("antlr")
            library("apache-jexl", "org.apache.commons:commons-jexl3:3.2.1")
            library("apache-text", "org.apache.commons:commons-text:1.9")
            library("klogger", "io.github.microutils:kotlin-logging-jvm:2.1.21")
            library("logback-core", "ch.qos.logback", "logback-core").versionRef("logback")
            library("logback-classic", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("reflections", "org.reflections:reflections:0.10.2")
        }
        create("testLibs") {
            version("cucumber", "7.0.0")
            library("cucumber-java8", "io.cucumber", "cucumber-java8").versionRef("cucumber")
            library("cucumber-junit", "io.cucumber", "cucumber-junit").versionRef("cucumber")
        }
    }
}

