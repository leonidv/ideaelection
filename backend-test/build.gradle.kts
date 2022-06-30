plugins {
    kotlin("jvm") version "1.7.0"
}


group = "ideaelection"
version = "1.0"

val kotestVersion = "5.3.1"
val jacksonVersion = "2.13.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.0")

    testImplementation("com.typesafe:config:1.4.2")

    testImplementation("org.codehaus.groovy:groovy:3.0.11")
    testImplementation("io.arrow-kt:arrow-core:1.1.2")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("com.jayway.jsonpath:json-path:2.7.0")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")

    testImplementation("ch.qos.logback:logback-classic:1.2.11")
    testImplementation("io.github.microutils:kotlin-logging:2.1.23")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.2.5")
}
//
//java {
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(17))
//        vendor.set(JvmVendorSpec.BELLSOFT)
//    }
//}

tasks {


    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }


    test {
        useJUnitPlatform()
    }
}
