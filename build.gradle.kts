plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
}

group = "ai.koog.book"
version = "0.0.1"

allprojects {
    repositories {
        mavenCentral()
    }
}
