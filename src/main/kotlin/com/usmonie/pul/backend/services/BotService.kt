package com.usmonie.pul.backend.services

import com.usmonie.pul.backend.database.Bots
import com.usmonie.pul.backend.models.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class BotService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Register a new bot
     */
    fun registerBot(request: BotRegistrationRequest): BotResponse {
        logger.info("Registering new bot: ${request.name}")

        return transaction {
            // Check for duplicate handle
            if (Bots.select(listOf(Bots.id))
                    .where { Bots.handle eq request.handle }
                    .count() > 0) {
                throw IllegalArgumentException("Bot with handle ${request.handle} already exists")
            }

            // Create new bot
            val bot = Bot(
                name = request.name,
                handle = request.handle,
                bankCode = request.bankCode,
                description = request.description,
                authType = request.authType,
                logoUrl = request.logoUrl,
                supportedFeatures = request.supportedFeatures
            )

            // Insert bot into database
            Bots.insert {
                it[id] = bot.id
                it[name] = bot.name
                it[handle] = bot.handle
                it[bankCode] = bot.bankCode
                it[description] = bot.description
                it[authType] = bot.authType
                it[credentials] = Json.encodeToString(request.credentials)
                it[logoUrl] = bot.logoUrl
                it[supportedFeatures] = Json.encodeToString(bot.supportedFeatures)
                it[createdAt] = bot.createdAt
            }

            logger.info("Bot registered successfully with ID: ${bot.id}")

            // Return response
            BotResponse(
                id = bot.id,
                name = bot.name,
                handle = bot.handle,
                bankCode = bot.bankCode,
                description = bot.description,
                authType = bot.authType,
                logoUrl = bot.logoUrl,
                supportedFeatures = bot.supportedFeatures,
                createdAt = bot.createdAt
            )
        }
    }

    /**
     * Get list of bots with pagination and sorting
     */
    fun getBots(limit: Int, offset: Long, sort: String): BotListResponse {
        logger.info("Getting bots with limit: $limit, offset: $offset, sort: $sort")

        if (limit < 1 || limit > 100 || offset < 0) {
            throw IllegalArgumentException("Invalid pagination parameters")
        }

        return transaction {
            // Determine sort column and order
            val (column, order) = when {
                sort.startsWith("-") -> {
                    val field = sort.substring(1)
                    when (field) {
                        "name" -> Pair(Bots.name, SortOrder.DESC)
                        "created_at" -> Pair(Bots.createdAt, SortOrder.DESC)
                        else -> Pair(Bots.name, SortOrder.ASC)
                    }
                }
                else -> {
                    when (sort) {
                        "name" -> Pair(Bots.name, SortOrder.ASC)
                        "created_at" -> Pair(Bots.createdAt, SortOrder.ASC)
                        else -> Pair(Bots.name, SortOrder.ASC)
                    }
                }
            }

            // Get total count
            val total = Bots.selectAll().count()

            // Get bots with pagination and sorting
            val allColumns = listOf(Bots.id, Bots.name, Bots.handle, Bots.bankCode, Bots.logoUrl)
            val bots = Bots.selectAll()
                .orderBy(column, order)
                .limit(limit)
                .offset(offset)
                .map {
                    BotListItem(
                        id = it[Bots.id],
                        name = it[Bots.name],
                        handle = it[Bots.handle],
                        bankCode = it[Bots.bankCode],
                        logoUrl = it[Bots.logoUrl]
                    )
                }

            BotListResponse(bots, total)
        }
    }

    /**
     * Get bot details by ID
     */
    fun getBot(botId: String): BotResponse {
        logger.info("Getting bot with ID: $botId")

        return transaction {
            val allColumns = listOf(
                Bots.id, Bots.name, Bots.handle, Bots.bankCode, Bots.description,
                Bots.authType, Bots.logoUrl, Bots.supportedFeatures, Bots.createdAt
            )

            val botRow = Bots.select(allColumns)
                .where { Bots.id eq botId }
                .singleOrNull()
                ?: throw NoSuchElementException("Bot not found with ID: $botId")

            val supportedFeatures = Json.decodeFromString<List<String>>(botRow[Bots.supportedFeatures])

            BotResponse(
                id = botRow[Bots.id],
                name = botRow[Bots.name],
                handle = botRow[Bots.handle],
                bankCode = botRow[Bots.bankCode],
                description = botRow[Bots.description],
                authType = botRow[Bots.authType],
                logoUrl = botRow[Bots.logoUrl],
                supportedFeatures = supportedFeatures,
                createdAt = botRow[Bots.createdAt]
            )
        }
    }

    /**
     * Update bot information
     */
    fun updateBot(botId: String, updateData: Map<String, String>): BotResponse {
        logger.info("Updating bot with ID: $botId")

        return transaction {
            val botExists = Bots.select(listOf(Bots.id))
                .where { Bots.id eq botId }
                .count() > 0

            if (!botExists) {
                throw NoSuchElementException("Bot not found with ID: $botId")
            }

            // Update fields
            Bots.update({ Bots.id eq botId }) {
                updateData["name"]?.let { name -> it[Bots.name] = name }
                updateData["description"]?.let { desc -> it[Bots.description] = desc }
                updateData["logo_url"]?.let { logo -> it[Bots.logoUrl] = logo }
            }

            // Return updated bot
            val allColumns = listOf(
                Bots.id, Bots.name, Bots.handle, Bots.bankCode, Bots.description,
                Bots.authType, Bots.logoUrl, Bots.supportedFeatures, Bots.createdAt
            )

            val updatedBot = Bots.select(allColumns)
                .where { Bots.id eq botId }
                .single()

            val supportedFeatures = Json.decodeFromString<List<String>>(updatedBot[Bots.supportedFeatures])

            BotResponse(
                id = updatedBot[Bots.id],
                name = updatedBot[Bots.name],
                handle = updatedBot[Bots.handle],
                bankCode = updatedBot[Bots.bankCode],
                description = updatedBot[Bots.description],
                authType = updatedBot[Bots.authType],
                logoUrl = updatedBot[Bots.logoUrl],
                supportedFeatures = supportedFeatures,
                createdAt = updatedBot[Bots.createdAt]
            )
        }
    }

    /**
     * Delete bot
     */
    fun deleteBot(botId: String) {
        logger.info("Deleting bot with ID: $botId")

        transaction {
            val result = Bots.deleteWhere { Bots.id eq botId }

            if (result == 0) {
                throw NoSuchElementException("Bot not found with ID: $botId")
            }

            logger.info("Bot deleted successfully")
        }
    }

    /**
     * Search bots by query and optional bank code
     */
    fun searchBots(query: String, limit: Int, bankCode: String?): List<BotListItem> {
        logger.info("Searching bots with query: '$query', limit: $limit, bankCode: $bankCode")

        if (limit < 1 || limit > 100) {
            throw IllegalArgumentException("Invalid limit parameter")
        }

        return transaction {
            val columnsToSelect = listOf(Bots.id, Bots.name, Bots.handle, Bots.bankCode, Bots.logoUrl)

            val conditions = listOf(
                (Bots.name like "%$query%") or (Bots.handle like "%$query%"),
                if (bankCode != null) (Bots.bankCode eq bankCode) else null
            ).filterNotNull()

            Bots.select(columnsToSelect)
                .where {
                    conditions.fold(Op.TRUE as Op<Boolean>) { acc, condition -> acc and condition }
                }
                .limit(limit)
                .map {
                    BotListItem(
                        id = it[Bots.id],
                        name = it[Bots.name],
                        handle = it[Bots.handle],
                        bankCode = it[Bots.bankCode],
                        logoUrl = it[Bots.logoUrl]
                    )
                }
        }
    }
}