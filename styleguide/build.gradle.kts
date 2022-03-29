plugins {
	@Suppress("DSL_SCOPE_VIOLATION")
	alias(libs.plugins.kotlin.jvm)
	@Suppress("DSL_SCOPE_VIOLATION")
	alias(libs.plugins.kotlin.spring)
	@Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.spring.boot)

	id("org.jetbrains.kotlin.kapt")
}

apply(plugin = "io.spring.dependency-management")

dependencies {

	// internal projects
	api(platform(project(":platform")))
	implementation(project(":runtime"))
	implementation(project(":default-fusion"))

	// spring
	compileOnly("org.springframework.boot:spring-boot-devtools")
	// annotationProcessor does not work here for IntelliJ
	// see https://stackoverflow.com/questions/37858833/spring-configuration-metadata-json-file-is-not-generated-in-intellij-idea-for-ko
	// see https://github.com/spring-io/initializr/issues/438
	kapt("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// utils
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	// logging
	implementation(libs.klogger)

	// testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named("compileKotlin") {
	dependsOn("processResources")
}

sourceSets {
	main {
		resources {
			srcDir("src/main/fusion")
		}
	}
}
