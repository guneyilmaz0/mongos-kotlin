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

group = "com.swadedev.mongos4k"
version = providers.gradleProperty("version").get()

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

// The library is Kotlin-only, so the javadoc JAR Maven Central requires carries the Dokka HTML instead.
tasks.named<Jar>("javadocJar") { from(tasks.named("dokkaGeneratePublicationHtml")) }
// Ship the license inside every JAR, as Maven Central consumers expect.
tasks.withType<Jar>().configureEach { from(rootProject.file("LICENSE")) { into("META-INF") } }
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
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
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("guneyilmaz0")
                        name.set("Güney Yılmaz")
                        email.set("guneyyilmaz2707@gmail.com")
                        url.set("https://github.com/guneyilmaz0")
                    }
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/guneyilmaz0/mongos-kotlin/issues")
                }

                scm {
                    connection.set("scm:git:git://github.com/guneyilmaz0/mongos-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/guneyilmaz0/mongos-kotlin.git")
                    url.set("https://github.com/guneyilmaz0/mongos-kotlin/tree/main")
                }
            }
        }
    }
    // A local Maven layout that the Central Portal bundle is built from; nothing is uploaded by Gradle.
    repositories { maven { name = "staging"; url = uri(layout.buildDirectory.dir("staging-deploy")) } }
}

// Signs only when an ASCII-armored key is supplied, for example through ORG_GRADLE_PROJECT_signingKey in CI.
signing {
    val key = providers.gradleProperty("signingKey").orNull
    if (key != null) {
        useInMemoryPgpKeys(key, providers.gradleProperty("signingPassword").orNull)
        sign(publishing.publications["maven"])
    }
}

tasks.register<Delete>("cleanStaging") { delete(layout.buildDirectory.dir("staging-deploy")) }
tasks.named("publishMavenPublicationToStagingRepository") { dependsOn("cleanStaging") }

tasks.register<Zip>("centralBundle") {
    group = "release"
    description = "Builds the Maven Central Portal upload bundle from the staging repository"
    dependsOn("publishAllPublicationsToStagingRepository")
    from(layout.buildDirectory.dir("staging-deploy"))
    exclude("**/maven-metadata*")
    archiveFileName.set("central-bundle-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}
