package com.usmonie.pul.backend.routes

import com.usmonie.pul.backend.models.*
import com.usmonie.pul.backend.services.BotService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.botRoutes() {
    val botService = BotService()

    routing {
        authenticate("auth-jwt") {
            route("/api/v1") {
                // 2.1. Register a new bot
                post("/bots") {
                    val request = call.receive<BotRegistrationRequest>()
                    val bot = botService.registerBot(request)
                    call.respond(HttpStatusCode.Created, bot)
                }

                // 2.2. Get list of bots
                get("/bots") {
                    val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                    val offset = call.parameters["offset"]?.toLongOrNull() ?: 0L
                    val sort = call.parameters["sort"] ?: "name"

                    val response = botService.getBots(limit, offset, sort)
                    call.respond(response)
                }

                // 2.3. Get bot details
                get("/bots/{bot_id}") {
                    val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                    val bot = botService.getBot(botId)
                    call.respond(bot)
                }

                // 2.4. Update bot information
                put("/bots/{bot_id}") {
                    val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                    val updateData = call.receive<Map<String, String>>()
                    val updatedBot = botService.updateBot(botId, updateData)
                    call.respond(updatedBot)
                }

                // 2.5. Delete bot
                delete("/bots/{bot_id}") {
                    val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                    botService.deleteBot(botId)
                    call.respond(HttpStatusCode.NoContent)
                }

                // 3. Bot search endpoint
                get("/search/bots") {
                    val query = call.parameters["q"] ?: ""
                    val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
                    val bankCode = call.parameters["bank_code"]

                    val results = botService.searchBots(query, limit, bankCode)
                    call.respond(mapOf("items" to results))
                }
            }
        }
    }
}