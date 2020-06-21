plugins {
    id("org.springframework.boot") version "2.3.1.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.spring") version "1.3.72"
    id("idea")
}

group = "ideaelection"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.security:spring-security-web")
    implementation("org.springframework.security:spring-security-config")

    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")


    implementation("com.couchbase.client:java-client:3.0.1")

    implementation("com.typesafe:config:1.4.0")
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.arrow-kt:arrow-core:0.10.4")

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.21")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:2.0.9")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:2.0.9")
}



tasks {
    test {
        useJUnitPlatform {
            includeEngines("spek2")
        }
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}