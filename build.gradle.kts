// build.gradle.kts for Backend Bank Bots API

// Plugin versions
val kotlinVersion = "2.1.20"
val ktorVersion = "3.1.2"
val exposedVersion = "0.61.0"
val logbackVersion = "1.4.6"
val hikariCpVersion = "5.0.1"
val postgresqlVersion = "42.5.4"
val h2Version = "2.1.214"

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "com.usmonie.pul.backend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Ktor (Server)
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")

    // Ktor (Authentication)
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.auth0:java-jwt:4.3.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Connection Pooling
    implementation("com.zaxxer:HikariCP:$hikariCpVersion")

    // Database Drivers
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.h2database:h2:$h2Version")  // For testing/development

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.slf4j:slf4j-api:2.0.5")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.mockk:mockk:1.13.4")
}

application {
    mainClass.set("com.usmonie.pul.backend.ApplicationKt")
}


kotlin {
    jvmToolchain(23)
}
// For packaging the application
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.usmonie.pul.backend.ApplicationKt"
    }

    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Include all runtime dependencies
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
