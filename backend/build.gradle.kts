val kotlinVersion = "1.7.0"
val jacksonVersion = "2.13.3"


plugins {
    id("org.springframework.boot") version "2.7.0"
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.spring") version "1.7.0"
    id("idea")
}

apply(plugin = "io.spring.dependency-management")

group = "saedi"
version = "1.0"

val kotestVersion= "5.3.1"
val springDocVersion = "1.6.9"
val exposedVersion = "0.38.2"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/konform-kt/konform")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.security:spring-security-web")
    implementation("org.springframework.security:spring-security-config")
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")

    implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")
    implementation("org.springdoc:springdoc-openapi-security:$springDocVersion")
    implementation("org.springdoc:springdoc-openapi-kotlin:$springDocVersion")

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.slf4j:jul-to-slf4j:1.7.36")


    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:42.3.6")
    implementation("com.zaxxer:HikariCP:5.0.1")

    implementation("org.liquibase:liquibase-core:4.11.0")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")

    implementation("io.konform:konform-jvm:0.4.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("io.arrow-kt:arrow-core:1.1.2")

    implementation("org.apache.commons:commons-lang3:3.12.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.2.5")
    testImplementation("io.kotest:kotest-assertions-konform-jvm:4.4.3")
    testImplementation("io.mockk:mockk:1.12.4")

}



tasks {
    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict", "-version")
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    bootJar {
        archiveFileName.set("idel-backend.jar")
    }
}

springBoot {
    buildInfo {
       properties {
           additional = mapOf(
               "commit" to "${System.getenv("GIT_COMMIT")}",
               "branch" to "${System.getenv("GIT_BRANCH")}"
           )
       }
    }
}
