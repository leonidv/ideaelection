val kotlinVersion = "1.4.0"
val jacksonVersion = "2.11.2"


plugins {
    id("org.springframework.boot") version "2.3.3.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.spring") version "1.4.0"
    id("idea")
}

group = "ideaelection"
version = "1.0-SNAPSHOT"

val kotestVersion="4.2.4"

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

    implementation("org.springframework.security:spring-security-web")
    implementation("org.springframework.security:spring-security-config")

    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    implementation("com.couchbase.client:java-client:3.1.2")
    implementation("com.couchbase.client:couchbase-transactions:1.1.5")

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
}