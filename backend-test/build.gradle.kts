plugins {
    kotlin("jvm") version "1.3.72"
}

group = "ideaelection"
version = "1.0"

val kotestVersion = "4.1.3"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("com.couchbase.client:java-client:3.0.1")
    testImplementation("com.typesafe:config:1.4.0")

    testImplementation("org.codehaus.groovy:groovy:3.0.0")
    testImplementation("io.rest-assured:rest-assured:4.2.0")
    testImplementation("io.rest-assured:kotlin-extensions:4.2.0")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.21")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-runner-console-jvm:$kotestVersion.2") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
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

        addTestListener(object : TestListener {
            override fun beforeTest(testDescriptor: TestDescriptor?) {}

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                println("${suite.name} total test: ${result.testCount}, failed: ${result.failedTestCount}")
            }

            override fun beforeSuite(suite: TestDescriptor?) {}

            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
                val name = testDescriptor.name!!
                if (name.startsWith("Describe:") ) {
                    println("${testDescriptor.name}")
                } else if (name.startsWith("It:") && result.failedTestCount > 0) {
                    val testName = name.substring(3)
                    println("FAILED: ${testDescriptor.parent!!.name}:${testName}")
                }
            }
        })
    }
}
