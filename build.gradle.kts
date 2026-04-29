import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.bcrypt)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("io.ktor:ktor-server-auth:3.4.0")
    implementation("io.ktor:ktor-server-auth-jwt:3.4.0")

    // Swagger
    implementation(libs.ktor.server.swagger)
    implementation("io.ktor:ktor-server-cors:3.4.0")
    implementation("io.ktor:ktor-server-metrics-micrometer:3.4.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.4")

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation("io.ktor:ktor-client-content-negotiation:3.4.0")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

}

tasks.test {
    // Тесты не требуют реальной БД — используется H2 in-memory
    // Поэтому никакие переменные окружения не нужны
    environment("KTOR_ENV", "test")
    useJUnit()
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
        )
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}