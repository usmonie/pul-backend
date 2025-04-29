package com.usmonie.pul.backend.models

import kotlinx.serialization.*
import java.time.Instant
import java.util.*

@Serializable
data class Bot(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val handle: String,
    val bankCode: String,
    val description: String,
    val authType: String,
    val logoUrl: String? = null,
    val supportedFeatures: List<String> = emptyList(),
    val createdAt: Long = Instant.now().epochSecond
)

@Serializable
data class BotCredentials(
    val clientId: String? = null,
    val clientSecret: String? = null,
    val authorizationUrl: String? = null,
    val tokenUrl: String? = null,
    val authEndpoint: String? = null,
    val usernameField: String? = null,
    val passwordField: String? = null,
    val apiKey: String? = null
)

@Serializable
data class BotRegistrationRequest(
    val name: String,
    val handle: String,
    val bankCode: String,
    val description: String,
    val authType: String,
    val credentials: BotCredentials,
    val logoUrl: String? = null,
    val supportedFeatures: List<String> = emptyList()
)

@Serializable
data class BotResponse(
    val id: String,
    val name: String,
    val handle: String,
    val bankCode: String,
    val description: String,
    val authType: String,
    val logoUrl: String? = null,
    val supportedFeatures: List<String> = emptyList(),
    val createdAt: Long
)

@Serializable
data class BotListItem(
    val id: String,
    val name: String,
    val handle: String,
    val bankCode: String,
    val logoUrl: String? = null
)

@Serializable
data class BotListResponse(
    val items: List<BotListItem>,
    val total: Long
)
