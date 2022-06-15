plugins {
    kotlin("jvm") version "1.4.30"
}


group = "ideaelection"
version = "1.0"

val kotestVersion = "4.6.0"
val jacksonVersion = "2.11.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.30")

    testImplementation("com.typesafe:config:1.4.0")

    testImplementation("org.codehaus.groovy:groovy:3.0.0")
    testImplementation("io.rest-assured:rest-assured:4.2.0")
    testImplementation("io.rest-assured:kotlin-extensions:4.2.0")

    testImplementation("io.arrow-kt:arrow-core:0.13.2")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("com.jayway.jsonpath:json-path:2.4.0")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.21")

    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("io.github.microutils:kotlin-logging:1.8.3")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.0.2")
}



tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }


    test {
        useJUnitPlatform()
    }
}
