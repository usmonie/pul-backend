package com.usmonie.pul.backend.models

import kotlinx.serialization.*

@Serializable
data class LoginPasswordAuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class ApiKeyAuthRequest(
    val apiKey: String
)

@Serializable
data class AuthResponse(
    val sessionToken: String? = null,
    val expiresIn: Int? = null,
    val authorizationUrl: String? = null
)
