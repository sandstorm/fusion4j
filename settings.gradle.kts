rootProject.name = "fusion4j"
include(
    "default-fusion",
    "platform",
    "lang",
    "runtime",
    "styleguide",
    "test-utils"
)

pluginManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }
}
