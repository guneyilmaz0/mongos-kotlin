plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    version.set("1.0.1")
    android.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

group = "net.guneyilmaz0.mongos4k"
version = "1.4.0"

repositories {
    mavenCentral()
}

dependencies {
    // Main dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.mongodb:mongodb-driver-sync:5.1.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    // Test dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.testcontainers:mongodb:1.19.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    // Test runtime
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Jar> {
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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("MongoS Kotlin")
                description.set("A lightweight and easy-to-use Kotlin wrapper for MongoDB operations")
                url.set("https://github.com/guneyilmaz0/mongos-kotlin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("guneyilmaz0")
                        name.set("Güney Ilmaz")
                        email.set("guneyilmaz0@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/guneyilmaz0/mongos-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com:guneyilmaz0/mongos-kotlin.git")
                    url.set("https://github.com/guneyilmaz0/mongos-kotlin/tree/main")
                }
            }
        }
    }
}
