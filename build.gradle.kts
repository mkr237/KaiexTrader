val logback_version: String by project
val koin_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.4.32"
    application
}

group = "kaiex"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.google.code.gson:gson:2.8.5")

    // Logback
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // KOIN
    implementation("io.insert-koin:koin-core:$koin_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // KTOR
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(18)
}

application {
    mainClass.set("MainKt")
}