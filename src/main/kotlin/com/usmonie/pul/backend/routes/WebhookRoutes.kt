package com.usmonie.pul.backend.routes

import com.usmonie.pul.backend.models.ErrorResponse
import com.usmonie.pul.backend.models.WebhookRegistration
import com.usmonie.pul.backend.services.WebhookService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.webhookRoutes() {
    val webhookService = WebhookService()

    routing {
        authenticate("auth-jwt") {
            route("/api/v1") {
                // 5.1. Register webhook
                post("/webhooks") {
                    val request = call.receive<WebhookRegistration>()
                    val webhook = webhookService.registerWebhook(request)
                    call.respond(HttpStatusCode.Created, webhook)
                }

                // Get webhooks for a bot
                get("/bots/{bot_id}/webhooks") {
                    val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                    val webhooks = webhookService.getWebhooksForBot(botId)
                    call.respond(webhooks)
                }

                // Delete webhook
                delete("/webhooks/{webhook_id}") {
                    val webhookId = call.parameters["webhook_id"] ?: throw IllegalArgumentException("Webhook ID is required")
                    webhookService.deleteWebhook(webhookId)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        // Webhook callback endpoint (no authentication required, validation via signature)
        post("/api/v1/webhook/{webhook_id}") {
            val webhookId = call.parameters["webhook_id"] ?: throw IllegalArgumentException("Webhook ID is required")
            val signature = call.request.header("X-Signature") ?: throw IllegalArgumentException("X-Signature header is required")

            val payload = call.receive<String>()

            // Validate signature
            if (!webhookService.validateSignature(webhookId, payload, signature)) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid_signature", "Invalid webhook signature"))
                return@post
            }

            // Process webhook
            webhookService.processWebhook(webhookId, payload)
            call.respond(HttpStatusCode.OK)
        }
    }
}
