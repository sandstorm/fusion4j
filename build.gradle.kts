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
}

evaluationDependsOnChildren()
subprojects {
    // publishing
    publishing {
        publications {
            create<MavenPublication>(project.name) {
                pom {
                    name.set("fusion4j - ${project.name}")
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
                    username = properties["NEXUS_USERNAME"] as String?
                    password = properties["NEXUS_PASSWORD"] as String?
                }
                val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                url = uri(if ((version as String).endsWith("-SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            }
        }

        signing {
            /*
             * Used in github action
             * SECRETS API ENV VARS:
             *  - ORG_GRADLE_PROJECT_signingKey
             *  - ORG_GRADLE_PROJECT_signingPassword
             */
            val inMemorySigning: Boolean = (project.properties["inMemorySigning"] as String?) == "true"
            if (inMemorySigning) {
                val signingKey: String by project
                val signingPassword: String by project
                useInMemoryPgpKeys(signingKey, signingPassword)
            }

            sign(publishing.publications[project.name])
        }
    }
}

task("printProjectVersion") {
    doFirst {
        println("version: ${project.version}")
    }
}

task("releaseSnapshot") {
    dependsOn("publish")
    doLast {
        println("CREATING SNAPSHOT RELEASE '${project.version}' ...")
    }
    onlyIf {
        // only build -> no publish
        // (accidentally pushed a non -SNAPSHOT version to main)
        val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")
        if (!isSnapshot) {
            println("NO SNAPSHOT RELEASE WILL BE CREATED -> version '${project.version}' is no snapshot")
        }
        isSnapshot
    }
}