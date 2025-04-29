package com.usmonie.pul.backend.database

import org.jetbrains.exposed.sql.*

object Bots : Table() {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val handle = varchar("handle", 50).uniqueIndex()
    val bankCode = varchar("bank_code", 10)
    val description = text("description")
    val authType = varchar("auth_type", 20)
    val credentials = text("credentials") // JSON serialized
    val logoUrl = varchar("logo_url", 255).nullable()
    val supportedFeatures = text("supported_features") // JSON serialized
    val createdAt = long("created_at")

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

object BotSessions : Table() {
    val id = varchar("id", 36)
    val botId = varchar("bot_id", 36).references(Bots.id)
    val userId = varchar("user_id", 36)
    val sessionToken = varchar("session_token", 255)
    val expiresAt = long("expires_at")
    override val primaryKey: PrimaryKey = PrimaryKey(id)

}

object Webhooks : Table() {
    val id = varchar("id", 36)
    val botId = varchar("bot_id", 36).references(Bots.id)
    val event = varchar("event", 50)
    val url = varchar("url", 255)
    val secretKey = varchar("secret_key", 64)
    val createdAt = long("created_at")
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
