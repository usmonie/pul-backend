package com.usmonie.pul.backend

import com.usmonie.pul.backend.database.DatabaseSetup
import com.usmonie.pul.backend.routes.*
import com.usmonie.pul.backend.security.RateLimit
import com.usmonie.pul.backend.security.configureJWT
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    // Initialize database
    DatabaseSetup.initDatabase()

    // Start server
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger(this::class.java)
    logger.info("Starting Bank Bots API...")

    // Install features
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Session-Token")
        anyHost()
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Error processing request", cause)
            when (cause) {
                is IllegalArgumentException -> call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "invalid_request", "error_description" to (cause.message ?: "Bad request"))
                )
                is NoSuchElementException -> call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "not_found", "error_description" to (cause.message ?: "Resource not found"))
                )
                else -> call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "internal_error", "error_description" to "Internal server error")
                )
            }
        }
    }

    // Configure JWT authentication
    configureJWT()

    // Configure rate limiting
//    install(RateLimit)

    // Register routes
    configureRoutes()

    logger.info("Bank Bots API started successfully")
}

fun Application.configureRoutes() {
    // Register all API routes
    botRoutes()
    authRoutes()
    accountRoutes()
    webhookRoutes()
}
