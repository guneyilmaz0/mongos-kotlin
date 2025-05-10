plugins {
    kotlin("jvm") version "2.1.20"
}

kotlin {
    jvmToolchain(21)
}

group = "net.guneyilmaz0.mongos4k"
version = "1.1.3"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.mongodb:mongodb-driver-sync:5.3.1")
    implementation("org.mongodb:bson:5.4.0")
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("ch.qos.logback:logback-classic:1.5.16")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "net.guneyilmaz0.MongoS"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.test {
    useJUnitPlatform()
}