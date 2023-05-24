import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

    implementation("com.swmansion.starknet:starknet:0.5.3")

    implementation("com.google.code.gson:gson:2.8.5")

    implementation(fileTree("libs") { include("*.jar") })
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Logback
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // KOIN
    implementation("io.insert-koin:koin-core:$koin_version")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")

    // KTOR CLIENT
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    // KTOR SERVER
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")

    // KTOR MISC
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")

    // TESTING
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:java-hamcrest:2.0.0.0")

    // STDLIB
    implementation(kotlin("stdlib-jdk8"))

    // STRATEGIES
    runtimeOnly(project(":strategies"))
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
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}