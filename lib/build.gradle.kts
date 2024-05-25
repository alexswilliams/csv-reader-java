@file:Suppress("UnstableApiUsage")

plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.apache.commons.text)
    implementation(libs.slf4j.api)
    implementation(libs.jetbrains.annotations)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useTestNG("7.5.1")
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(8)
}
