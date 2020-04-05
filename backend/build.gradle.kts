plugins {
    id("org.springframework.boot") version "2.2.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.spring") version "1.3.61"
}

group = "ideaelection"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.couchbase.client:java-client:3.0.1")

    implementation("com.typesafe:config:1.4.0")

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
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

//sourceSets {
//    main {
//        java {
//            srcDir("src/main/kotlin")
//        }
//    }
//    test {
//        java {
//            srcDir("src/test/kotlin")
//        }
//    }
//}