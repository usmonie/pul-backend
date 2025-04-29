package com.usmonie.pul.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

fun Application.configureJWT() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()
    
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )
            validate { credential ->
                // You can check against a user database here
                // For now, we'll just validate the token
                JWTPrincipal(credential.payload)
            }
        }
    }
}

class JwtService(
    private val secret: String,
    private val issuer: String,
    private val audience: String
) {
    fun generateToken(botId: String, userId: String, expiresInSeconds: Long = 3600): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("bot_id", botId)
            .withClaim("user_id", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
            .sign(Algorithm.HMAC256(secret))
    }
}
