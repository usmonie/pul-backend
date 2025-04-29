package com.usmonie.pul.backend.database

import com.usmonie.pul.backend.models.Bot
import com.usmonie.pul.backend.models.BotCredentials
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*
import java.time.Instant

object DatabaseSetup {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    fun initDatabase(
        url: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/bank_bots",
        driver: String = System.getenv("DATABASE_DRIVER") ?: "org.postgresql.Driver",
        user: String = System.getenv("DATABASE_USER") ?: "postgres",
        password: String = System.getenv("DATABASE_PASSWORD") ?: "postgres"
    ) {
        logger.info("Initializing database with URL: $url")
        
        // Connect to database
        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )
        
        // Create tables
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Bots,
                BotSessions,
                Webhooks
            )
        }
        
        logger.info("Database initialized successfully")
        
        // Load sample data if needed
        val loadSampleData = System.getenv("LOAD_SAMPLE_DATA")?.toBoolean() ?: false
        if (loadSampleData) {
            logger.info("Loading sample data...")
            populateSampleData()
        }
    }
    
    private fun populateSampleData() {
        transaction {
            // Check if we already have data
            val botCount = Bots.selectAll().count()
            if (botCount > 0) {
                logger.info("Database already contains data, skipping sample data population")
                return@transaction
            }
            
            // Sample bots
            val sampleBots = listOf(
                Pair(
                    Bot(
                        name = "Сбербанк",
                        handle = "@sberbank",
                        bankCode = "SBER",
                        description = "Официальный бот Сбербанка для выгрузки счетов и транзакций",
                        authType = "oauth2",
                        logoUrl = "https://www.sberbank.ru/static/logo.png",
                        supportedFeatures = listOf("accounts", "transactions", "balance")
                    ),
                    BotCredentials(
                        clientId = "sber-client-id",
                        clientSecret = "sber-client-secret",
                        authorizationUrl = "https://api.sberbank.ru/oauth/authorize",
                        tokenUrl = "https://api.sberbank.ru/oauth/token"
                    )
                ),
                Pair(
                    Bot(
                        name = "Тинькофф",
                        handle = "@tinkoff",
                        bankCode = "TINK",
                        description = "Официальный бот Тинькофф Банка для выгрузки счетов и транзакций",
                        authType = "api_key",
                        logoUrl = "https://www.tinkoff.ru/static/logo.png",
                        supportedFeatures = listOf("accounts", "transactions", "balance", "categories")
                    ),
                    BotCredentials(
                        apiKey = "sample-api-key-field"
                    )
                ),
                Pair(
                    Bot(
                        name = "ВТБ",
                        handle = "@vtb",
                        bankCode = "VTB",
                        description = "Официальный бот ВТБ для выгрузки счетов и транзакций",
                        authType = "login_password",
                        logoUrl = "https://www.vtb.ru/static/logo.png",
                        supportedFeatures = listOf("accounts", "transactions")
                    ),
                    BotCredentials(
                        authEndpoint = "https://api.vtb.ru/auth",
                        usernameField = "login",
                        passwordField = "password"
                    )
                )
            )
            
            // Insert sample bots
            sampleBots.forEach { (bot, credentials) ->
                Bots.insert {
                    it[id] = bot.id
                    it[name] = bot.name
                    it[handle] = bot.handle
                    it[bankCode] = bot.bankCode
                    it[description] = bot.description
                    it[authType] = bot.authType
                    it[Bots.credentials] = Json.encodeToString(credentials)
                    it[logoUrl] = bot.logoUrl
                    it[supportedFeatures] = Json.encodeToString(bot.supportedFeatures)
                    it[createdAt] = bot.createdAt
                }
            }
            
            logger.info("Sample data populated successfully")
        }
    }
}
