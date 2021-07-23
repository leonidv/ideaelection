buildscript {
    dependencies {
        classpath("io.qameta.allure:allure-gradle:2.8.1")
    }
}


plugins {
    kotlin("jvm") version "1.4.30"
    id("io.qameta.allure") version("2.8.1")
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
    implementation("io.kotest.extensions:kotest-extensions-allure:1.0.1")
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
//        addTestListener(object : TestListener {
//            override fun beforeTest(testDescriptor: TestDescriptor?) {}
//
//            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
//                println("${suite.name} total test: ${result.testCount}, failed: ${result.failedTestCount}")
//            }
//
//            override fun beforeSuite(suite: TestDescriptor?) {}
//
//            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
//                val name = testDescriptor.name!!
//                if (name.startsWith("Describe:") ) {
//                    println("${testDescriptor.name}")
//                } else if (name.startsWith("It:") && result.failedTestCount > 0) {
//                    val testName = name.substring(3)
//                    println("FAILED: ${testDescriptor.parent!!.name}:${testName}")
//                }
//            }
//        })
    }
}

allure {
    autoconfigure = false
    version = "2.13.8"
}
