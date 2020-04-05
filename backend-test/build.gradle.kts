plugins {
    kotlin("jvm") version "1.3.61"
}

group = "ideaelection"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("com.couchbase.client:java-client:3.0.1")
    testImplementation("com.typesafe:config:1.4.0")

    testImplementation("org.codehaus.groovy:groovy:3.0.0")
    testImplementation("io.rest-assured:rest-assured:4.2.0")
    testImplementation("io.rest-assured:kotlin-extensions:4.2.0")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.21")

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:2.0.9")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:2.0.9")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    test {
        useJUnitPlatform {
            includeEngines("spek2")
        }
    }
}