package com.usmonie.pul.backend.services

import com.usmonie.pul.backend.database.Bots
import com.usmonie.pul.backend.database.Webhooks
import com.usmonie.pul.backend.models.WebhookPayload
import com.usmonie.pul.backend.models.WebhookRegistration
import com.usmonie.pul.backend.models.WebhookResponse
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Register a new webhook
     */
    fun registerWebhook(request: WebhookRegistration): WebhookResponse {
        logger.info("Registering webhook for bot ${request.botId} and event ${request.event}")

        return transaction {
            // Verify bot exists
            val botExists = Bots.select(listOf(Bots.id))
                .where { Bots.id eq request.botId }
                .count() > 0

            if (!botExists) {
                throw NoSuchElementException("Bot not found with ID: ${request.botId}")
            }

            // Generate secret key
            val secretKey = UUID.randomUUID().toString().replace("-", "")

            // Create webhook
            val webhookId = UUID.randomUUID().toString()
            val now = Instant.now().epochSecond

            Webhooks.insert {
                it[id] = webhookId
                it[botId] = request.botId
                it[event] = request.event
                it[url] = request.url
                it[Webhooks.secretKey] = secretKey
                it[createdAt] = now
            }

            WebhookResponse(
                webhookId = webhookId,
                botId = request.botId,
                event = request.event,
                url = request.url,
                secretKey = secretKey,
                createdAt = now
            )
        }
    }

    /**
     * Get webhooks for a bot
     */
    fun getWebhooksForBot(botId: String): List<WebhookResponse> {
        logger.info("Getting webhooks for bot $botId")

        return transaction {
            val allColumns = listOf(
                Webhooks.id, Webhooks.botId, Webhooks.event,
                Webhooks.url, Webhooks.secretKey, Webhooks.createdAt
            )

            Webhooks.select(allColumns)
                .where { Webhooks.botId eq botId }
                .map {
                    WebhookResponse(
                        webhookId = it[Webhooks.id],
                        botId = it[Webhooks.botId],
                        event = it[Webhooks.event],
                        url = it[Webhooks.url],
                        secretKey = it[Webhooks.secretKey],
                        createdAt = it[Webhooks.createdAt]
                    )
                }
        }
    }

    /**
     * Delete a webhook
     */
    fun deleteWebhook(webhookId: String) {
        logger.info("Deleting webhook $webhookId")

        transaction {
            val result = Webhooks.deleteWhere { Webhooks.id eq webhookId }

            if (result == 0) {
                throw NoSuchElementException("Webhook not found with ID: $webhookId")
            }
        }
    }

    /**
     * Validate webhook signature
     */
    fun validateSignature(webhookId: String, payload: String, signature: String): Boolean {
        logger.info("Validating signature for webhook $webhookId")

        val secretKey = transaction {
            Webhooks.select(listOf(Webhooks.secretKey))
                .where { Webhooks.id eq webhookId }
                .map { it[Webhooks.secretKey] }
                .singleOrNull() ?: return@transaction null
        } ?: return false

        return calculateHmacSha256(payload, secretKey) == signature
    }

    /**
     * Process incoming webhook
     */
    fun processWebhook(webhookId: String, payload: String) {
        logger.info("Processing webhook $webhookId")

        // In a real implementation, you would process the webhook data
        // and potentially trigger notifications or other actions

        try {
            val webhookData = json.decodeFromString<WebhookPayload>(payload)
            logger.info("Webhook event: ${webhookData.event} for account ${webhookData.accountId}")

            // Process based on event type
            when (webhookData.event) {
                "transaction.created" -> {
                    logger.info("New transaction detected")
                    // Process new transaction
                }
                "balance.changed" -> {
                    logger.info("Balance changed")
                    // Process balance change
                }
                else -> {
                    logger.info("Unknown event type: ${webhookData.event}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing webhook: ${e.message}")
        }
    }

    /**
     * Trigger webhook to a registered URL
     */
    fun triggerWebhook(botId: String, accountId: String, event: String, data: Map<String, Any>) {
        logger.info("Triggering webhooks for bot $botId, account $accountId, event $event")

        val allColumns = listOf(
            Webhooks.id, Webhooks.botId, Webhooks.event,
            Webhooks.url, Webhooks.secretKey, Webhooks.createdAt
        )

        val webhooks = transaction {
            Webhooks.select(allColumns)
                .where { (Webhooks.botId eq botId) and (Webhooks.event eq event) }
                .map {
                    WebhookResponse(
                        webhookId = it[Webhooks.id],
                        botId = it[Webhooks.botId],
                        event = it[Webhooks.event],
                        url = it[Webhooks.url],
                        secretKey = it[Webhooks.secretKey],
                        createdAt = it[Webhooks.createdAt]
                    )
                }
        }

        if (webhooks.isEmpty()) {
            logger.info("No webhooks registered for this event")
            return
        }

        // Prepare payload
        val payload = WebhookPayload(
            botId = botId,
            accountId = accountId,
            event = event,
            data = data
        )

        val payloadJson = json.encodeToString(payload)

        // Send webhook to each registered URL
        webhooks.forEach { webhook ->
            try {
                val signature = calculateHmacSha256(payloadJson, webhook.secretKey)

                // In a real implementation, you would use an HTTP client to send the webhook
                // For example, using Ktor client:
                /*
                val client = HttpClient()
                client.post(webhook.url) {
                    header("Content-Type", "application/json")
                    header("X-Signature", signature)
                    body = payloadJson
                }
                */

                logger.info("Webhook sent to ${webhook.url}")
            } catch (e: Exception) {
                logger.error("Error sending webhook to ${webhook.url}: ${e.message}")
            }
        }
    }

    /**
     * Calculate HMAC-SHA256 signature
     */
    private fun calculateHmacSha256(data: String, key: String): String {
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val bytes = mac.doFinal(data.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}