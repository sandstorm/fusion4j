description = "fusion4j - Platform"

plugins {
    `java-platform`
}

configure<PublishingExtension> {
    publications {
        withType<MavenPublication> {
            from(components["javaPlatform"])

            pom {
                packaging = "bom"
            }
        }
    }
}