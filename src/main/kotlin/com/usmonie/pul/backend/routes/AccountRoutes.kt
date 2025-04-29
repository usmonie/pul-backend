package com.usmonie.pul.backend.routes

import com.usmonie.pul.backend.services.AccountService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*

fun Application.accountRoutes() {
    val accountService = AccountService()

    routing {
        route("/api/v1") {
            // 4.2. Get user accounts
            get("/bots/{bot_id}/accounts") {
                val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                val sessionToken = call.request.header("X-Session-Token")
                    ?: throw IllegalArgumentException("X-Session-Token header is required")

                val accounts = accountService.getAccounts(botId, sessionToken)
                call.respond(accounts)
            }

            // 4.3. Get account balance
            get("/bots/{bot_id}/accounts/{account_id}/balance") {
                val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                val accountId = call.parameters["account_id"] ?: throw IllegalArgumentException("Account ID is required")
                val sessionToken = call.request.header("X-Session-Token")
                    ?: throw IllegalArgumentException("X-Session-Token header is required")

                val balance = accountService.getAccountBalance(botId, accountId, sessionToken)
                call.respond(balance)
            }

            // 4.4. Get account transactions
            get("/bots/{bot_id}/accounts/{account_id}/transactions") {
                val botId = call.parameters["bot_id"] ?: throw IllegalArgumentException("Bot ID is required")
                val accountId = call.parameters["account_id"] ?: throw IllegalArgumentException("Account ID is required")
                val sessionToken = call.request.header("X-Session-Token")
                    ?: throw IllegalArgumentException("X-Session-Token header is required")

                // Parse query parameters
                val from = call.parameters["from"]
                val to = call.parameters["to"]
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val type = call.parameters["type"]

                val transactions = accountService.getTransactions(botId, accountId, sessionToken, from, to, limit, type)
                call.respond(transactions)
            }
        }
    }
}
