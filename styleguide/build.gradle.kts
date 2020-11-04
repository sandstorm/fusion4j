import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	//java
	kotlin("jvm")
	kotlin("kapt")
	id("org.springframework.boot")
	id("io.spring.dependency-management")
	kotlin("plugin.spring")
}

repositories {
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	api(platform(project(":platform")))

	implementation(project(":runtime"))
	implementation(project(":default-fusion"))

	compileOnly("org.springframework.boot:spring-boot-devtools")

	// annotationProcessor does not work here for IntelliJ
	// see https://stackoverflow.com/questions/37858833/spring-configuration-metadata-json-file-is-not-generated-in-intellij-idea-for-ko
	// see https://github.com/spring-io/initializr/issues/438
	kapt("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-configuration-processor")

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	implementation("io.github.microutils:kotlin-logging-jvm")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
	}
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
