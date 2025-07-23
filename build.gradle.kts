plugins {
    kotlin("jvm") version "2.0.0"
}

kotlin {
    jvmToolchain(17)
}

group = "net.guneyilmaz0.mongos4k"
version = "1.2.2"

repositories {
    mavenCentral()
}

dependencies {
    // Main dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.mongodb:mongodb-driver-sync:5.1.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}