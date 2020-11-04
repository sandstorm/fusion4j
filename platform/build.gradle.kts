plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api("org.antlr", "antlr4-runtime", project.ext.get("antlrVersion") as String)
        api("io.projectreactor:reactor-core:3.4.0")
        api("io.github.microutils:kotlin-logging-jvm:2.1.21")
        api("ch.qos.logback:logback-core:1.2.10")
        api("ch.qos.logback:logback-classic:1.2.10")
        api("org.reflections:reflections:0.10.2")
        api("org.apache.commons:commons-jexl3:3.2.1")
    }
}