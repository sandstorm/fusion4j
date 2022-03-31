description = "fusion4j - Styleguide Spring application"

plugins {
	@Suppress("DSL_SCOPE_VIOLATION")
	alias(libs.plugins.kotlin.jvm)
	@Suppress("DSL_SCOPE_VIOLATION")
	alias(libs.plugins.kotlin.spring)
	@Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.spring.boot)

	id("org.jetbrains.kotlin.kapt")
}

// bom creation error, see: https://github.com/jfrog/build-info/issues/198
//apply(plugin = "io.spring.dependency-management")

dependencies {

	// internal projects
	api(platform(project(":platform")))
	implementation(project(":runtime"))
	implementation(project(":default-fusion"))

	// spring
	compileOnly("org.springframework.boot:spring-boot-devtools:${libs.versions.spring.get()}")
	// annotationProcessor does not work here for IntelliJ
	// see https://stackoverflow.com/questions/37858833/spring-configuration-metadata-json-file-is-not-generated-in-intellij-idea-for-ko
	// see https://github.com/spring-io/initializr/issues/438
	kapt("org.springframework.boot:spring-boot-configuration-processor:${libs.versions.spring.get()}")
	implementation("org.springframework.boot:spring-boot-configuration-processor:${libs.versions.spring.get()}")
	implementation("org.springframework.boot:spring-boot-starter-web:${libs.versions.spring.get()}")

	// utils
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	// logging
	implementation(libs.klogger)
	implementation(libs.logback.core)
	implementation(libs.logback.classic)

	// testing
	testImplementation("org.springframework.boot:spring-boot-starter-test:${libs.versions.spring.get()}")
}

sourceSets {
	main {
		resources {
			srcDir("src/main/fusion")
		}
	}
}

// see https://github.com/gradle/gradle/issues/17236#issuecomment-870525957
gradle.taskGraph.whenReady {
	allTasks
		.filter { it.hasProperty("duplicatesStrategy") } // Because it's some weird decorated wrapper that I can't cast.
		.forEach {
			it.setProperty("duplicatesStrategy", "EXCLUDE")
		}
}

java {
	withSourcesJar()
	withJavadocJar()
}

tasks.named("compileKotlin") {
	dependsOn("processResources")
}

configure<PublishingExtension> {
	publications {
		withType<MavenPublication> {
			from(components["java"])
		}
	}
}