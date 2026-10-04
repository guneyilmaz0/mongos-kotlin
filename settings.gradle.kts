pluginManagement {
    repositories {
        // Mirror first: Maven Central sometimes answers 403 to CI runners.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "mongos-kotlin"
