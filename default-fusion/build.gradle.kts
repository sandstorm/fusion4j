plugins {
    `java-library`
    id("kotlin")
}

dependencies {
    api(platform(project(":platform")))
    api(project(":lang"))
    api(project(":runtime"))

    implementation("io.github.microutils:kotlin-logging-jvm")
    runtimeOnly("ch.qos.logback:logback-core")
    runtimeOnly("ch.qos.logback:logback-classic")

    implementation("org.apache.commons:commons-text:1.9")

    testImplementation(platform(project(":test-utils")))
}

sourceSets {
    main {
        resources {
            srcDir("src/main/fusion")
        }
    }
}

tasks.jar.configure {
    actions = emptyList()
    dependsOn(listOf("neosFusionJar"))
}

// Neos.Fusion
task("neosFusionJar", Jar::class) {
    archiveAppendix.set("Neos.Fusion")
    from(sourceSets.main.get().output) {
        include("io/neos/fusion4j/neos/fusion/*")
        include("Neos.Fusion/*")
    }
}

// Neos.Neos
task("neosNeosJar", Jar::class) {
    archiveAppendix.set("Neos.Neos")
    from(sourceSets.main.get().output) {
        include("io/neos/fusion4j/neos/neos/*")
        include("Neos.Neos/*")
    }
}

// Neos.NodeTypes
task("neosNodeTypesJar", Jar::class) {
    archiveAppendix.set("Neos.NodeTypes")
    from(sourceSets.main.get().output) {
        include("io/neos/fusion4j/neos/nodetypes/*")
        include("Neos.NodeTypes/*")
    }
}
