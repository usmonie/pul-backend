package com.usmonie.pul.backend.models

import kotlinx.serialization.*
import java.time.Instant

@Serializable
data class WebhookRegistration(
    val botId: String,
    val event: String,
    val url: String
)

@Serializable
data class WebhookResponse(
    val webhookId: String,
    val botId: String,
    val event: String,
    val url: String,
    val secretKey: String,
    val createdAt: Long = Instant.now().epochSecond
)

@Serializable
data class WebhookPayload(
    val botId: String,
    val accountId: String,
    val event: String,
    val data: Map<String, @Contextual Any>
)
