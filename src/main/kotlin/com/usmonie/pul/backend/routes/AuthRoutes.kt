package com.usmonie.pul.backend.routes

import com.usmonie.pul.backend.models.ApiKeyAuthRequest
import com.usmonie.pul.backend.models.LoginPasswordAuthRequest
import com.usmonie.pul.backend.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.authRoutes() {
    val authService = AuthService()

    routing {
        authenticate("auth-jwt") {
            route("/api/v1") {
                // 4.1. Authorize user with bot
                post("/bots/{bot_id}/authorize") {
                    val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject ?: throw IllegalArgumentException("User ID not found in token")

                    // Determine authorization type based on bot configuration
                    val authType = authService.getBotAuthType(botId)

                    when (authType) {
                        "login_password" -> {
                            val request = call.receive<LoginPasswordAuthRequest>()
                            val response = authService.authorizeWithLoginPassword(botId, userId, request.username, request.password)
                            call.respond(response)
                        }
                        "api_key" -> {
                            val request = call.receive<ApiKeyAuthRequest>()
                            val response = authService.authorizeWithApiKey(botId, userId, request.apiKey)
                            call.respond(response)
                        }
                        "oauth2" -> {
                            val response = authService.startOAuth2Authorization(botId, userId)
                            call.respond(response)
                        }
                        else -> {
                            throw IllegalArgumentException("Unsupported authentication type: $authType")
                        }
                    }
                }

                // OAuth2 callback handler
                get("/oauth/callback") {
                    val code = call.parameters["code"] ?: throw IllegalArgumentException("Authorization code is required")
                    val state = call.parameters["state"] ?: throw IllegalArgumentException("State parameter is required")

                    // The state parameter should contain the bot ID and user ID
                    val (botId, userId) = authService.parseOAuthState(state)

                    val response = authService.completeOAuth2Authorization(botId, userId, code)

                    // Redirect to the mobile app using a deep link
                    call.respondRedirect("yourapp://oauth/callback?session_token=${response.sessionToken}")
                }
            }
        }
    }
}