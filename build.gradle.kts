import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "fusion4j - root project"

plugins {
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
}

allprojects {
    group = "io.neos.fusion4j"

    tasks {
        withType<KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = "17"
            sourceCompatibility = JavaVersion.VERSION_17.toString()
            targetCompatibility = JavaVersion.VERSION_17.toString()
        }

        withType<Test>().configureEach {
            useJUnit()
        }

        withType<Copy> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    configurations.all {
        exclude(module = "log4j-to-slf4j")
    }
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    // publishing
    publishing {
        publications {
            create<MavenPublication>(project.name) {
                pom {
                    //packaging = "jar"
                    //name.set("<libraryname>")
                    description.set(project.description)
                    url.set("https://github.com/sandstorm/fusion4j")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://github.com/sandstorm/fusion4j/blob/main/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.set("eric.kloss")
                            name.set("Eric Kloss")
                            email.set("eric.kloss@sandstorm.de")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/sandstorm/fusion4j.git")
                        developerConnection.set("scm:git:ssh://github.com/sandstorm/fusion4j.git")
                        url.set("https://github.com/sandstorm/fusion4j")
                    }
                }
            }
        }

        repositories {
            maven {
                credentials {
                    username = properties["NEXUS_USERNAME"] as String
                    password = properties["NEXUS_PASSWORD"] as String
                }
                val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                url = uri(if ((version as String).endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            }
        }

        signing {
            sign(publishing.publications[project.name])
        }
    }
}
