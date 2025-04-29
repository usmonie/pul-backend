package com.usmonie.pul.backend.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.usmonie.pul.backend.database.BotSessions
import com.usmonie.pul.backend.database.Bots
import com.usmonie.pul.backend.models.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*
import java.util.Base64

class AuthService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // Config values normally would be injected
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "development-secret"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "bank-bots-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "bank-bots-users"

    /**
     * Get bot authentication type
     */
    fun getBotAuthType(botId: String): String {
        logger.info("Getting auth type for bot: $botId")

        return transaction {
            val bot = Bots.select(listOf(Bots.authType))
                .where { Bots.id eq botId }
                .singleOrNull()
                    ?: throw NoSuchElementException("Bot not found with ID: $botId")

                bot[Bots.authType]
            }
        }

        /**
         * Authorize user with bot using login/password
         */
        fun authorizeWithLoginPassword(botId: String, userId: String, username: String, password: String): AuthResponse {
            logger.info("Authorizing user $userId with bot $botId using login/password")

            return authResponse(botId, userId)
        }

        /**
         * Authorize user with bot using API key
         */
        fun authorizeWithApiKey(botId: String, userId: String, apiKey: String): AuthResponse {
            logger.info("Authorizing user $userId with bot $botId using API key")

            return authResponse(botId, userId)
        }

        private fun authResponse(botId: String, userId: String) = transaction {
            val botRow = Bots.select(listOf(Bots.credentials))
                .where { Bots.id eq botId }
                .singleOrNull()
                ?: throw NoSuchElementException("Bot not found with ID: $botId")

            val credentials = Json.decodeFromString<BotCredentials>(botRow[Bots.credentials])

            // In a real application, you would verify the API key against the bank's API
            // For now, we'll just create a session token

            val sessionToken = createSessionToken(botId, userId)
            val expiresIn = 3600 // 1 hour

            // Store session
            val sessionId = UUID.randomUUID().toString()
            BotSessions.insert {
                it[id] = sessionId
                it[BotSessions.botId] = botId
                it[BotSessions.userId] = userId
                it[BotSessions.sessionToken] = sessionToken
                it[expiresAt] = System.currentTimeMillis() / 1000 + expiresIn
            }

            AuthResponse(
                sessionToken = sessionToken,
                expiresIn = expiresIn
            )
        }

        /**
         * Start OAuth2 authorization flow
         */
        fun startOAuth2Authorization(botId: String, userId: String): AuthResponse {
            logger.info("Starting OAuth2 authorization for user $userId with bot $botId")

            return transaction {
                val botRow = Bots.select(listOf(Bots.credentials))
                    .where { Bots.id eq botId }
                    .singleOrNull()
                    ?: throw NoSuchElementException("Bot not found with ID: $botId")

                val credentials = Json.decodeFromString<BotCredentials>(botRow[Bots.credentials])

                val authUrl = credentials.authorizationUrl
                    ?: throw IllegalStateException("OAuth2 authorization URL not configured for this bot")

                // Create state parameter for CSRF protection
                val state = encodeOAuthState(botId, userId)

                // Construct full authorization URL with parameters
                val fullAuthUrl = buildString {
                    append(authUrl)
                    append(if (authUrl.contains("?")) "&" else "?")
                    append("response_type=code")
                    append("&client_id=${credentials.clientId}")
                    append("&redirect_uri=https://api.yourapp.com/api/v1/oauth/callback")
                    append("&state=$state")
                    append("&scope=read") // Adjust scope as needed
                }

                AuthResponse(
                    authorizationUrl = fullAuthUrl
                )
            }
        }

        /**
         * Complete OAuth2 authorization flow
         */
        fun completeOAuth2Authorization(botId: String, userId: String, authCode: String): AuthResponse {
            logger.info("Completing OAuth2 authorization for user $userId with bot $botId")

            return authResponse(botId, userId)
        }

        /**
         * Encode OAuth state parameter (botId + userId)
         */
        fun encodeOAuthState(botId: String, userId: String): String {
            val stateData = "$botId:$userId"
            return Base64.getUrlEncoder().encodeToString(stateData.toByteArray())
        }

        /**
         * Parse OAuth state parameter
         */
        fun parseOAuthState(state: String): Pair<String, String> {
            val stateData = String(Base64.getUrlDecoder().decode(state))
            val parts = stateData.split(":")

            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid state parameter")
            }

            return Pair(parts[0], parts[1])
        }

        /**
         * Create a session token
         */
        private fun createSessionToken(botId: String, userId: String): String {
            return JWT.create()
                .withIssuer(jwtIssuer)
                .withAudience(jwtAudience)
                .withClaim("bot_id", botId)
                .withClaim("user_id", userId)
                .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 1000))
                .sign(Algorithm.HMAC256(jwtSecret))
        }

        /**
         * Validate session token
         */
        fun validateSessionToken(token: String, botId: String): Boolean {
            logger.info("Validating session token for bot $botId")

            return transaction {
                val session = BotSessions.select(listOf(BotSessions.expiresAt))
                    .where { (BotSessions.sessionToken eq token) and (BotSessions.botId eq botId) }
                    .singleOrNull() ?: return@transaction false

                val now = System.currentTimeMillis() / 1000
                session[BotSessions.expiresAt] >= now
            }
        }
    }