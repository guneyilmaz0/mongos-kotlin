plugins {
    kotlin("jvm") version "2.4.20"
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "2.2.0"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-jvm-default=no-compatibility", "-Xjsr305=strict")
    }
}

// Java 17 bytecode and API level so the library works for Java 17+ consumers.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

group = "net.guneyilmaz0.mongos4k"
version = "1.7.0"

repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    mavenCentral()
}

dependencies {
    // Exposed in the public API (MongoCollection, Document, Gson), so consumers get them transitively.
    api("org.mongodb:mongodb-driver-sync:5.13.0")
    api("com.google.code.gson:gson:2.14.0")
    // Logging facade only; consumers bring their own backend.
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.6.5")
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
                        name.set("Güney Yılmaz")
                        email.set("guneyyilmaz2707@gmail.com")
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
