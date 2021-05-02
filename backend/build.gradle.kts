val kotlinVersion = "1.4.30"
val jacksonVersion = "2.11.2"


plugins {
    id("org.springframework.boot") version "2.4.4"
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.spring") version "1.4.30"
    id("idea")
}

apply(plugin = "io.spring.dependency-management")

group = "saedi"
version = "1.0"

val kotestVersion="4.2.4"
val springDocVersion = "1.5.8"

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

    implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")
    implementation("org.springdoc:springdoc-openapi-security:$springDocVersion")
    implementation("org.springdoc:springdoc-openapi-kotlin:$springDocVersion")


    implementation("com.couchbase.client:java-client:3.1.4")
    implementation("com.couchbase.client:couchbase-transactions:1.1.6")
    implementation("io.projectreactor:reactor-core:3.4.3") // https://github.com/Azure/azure-sdk-for-java/issues/20106


    implementation("io.github.microutils:kotlin-logging:1.7.9")

    implementation("io.konform:konform-jvm:0.2.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("io.arrow-kt:arrow-core:0.10.4")

    implementation("org.apache.commons:commons-lang3:3.10")

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.21")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-assertions-arrow-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-konform-jvm:$kotestVersion")
    testImplementation("io.mockk:mockk:1.10.2")

}



tasks {
    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
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