package com.usmonie.pul.backend.utils

/**
 * Utility class for input validation
 */
object ValidationUtils {

    /**
     * Validate a bot handle (should start with @ and contain only alphanumeric characters, underscores, and hyphens)
     */
    fun isValidBotHandle(handle: String): Boolean {
        return handle.startsWith("@") &&
               handle.substring(1).matches(Regex("^[a-zA-Z0-9_-]+$")) &&
               handle.length >= 3 && handle.length <= 50
    }

    /**
     * Validate a bank code (should be uppercase and contain only alphabetic characters)
     */
    fun isValidBankCode(bankCode: String): Boolean {
        return bankCode.matches(Regex("^[A-Z]+$")) &&
               bankCode.length >= 2 && bankCode.length <= 10
    }

    /**
     * Validate a URL (simple regex validation)
     */
    fun isValidUrl(url: String): Boolean {
        return url.matches(Regex("^(https?)://[^\\s/$.?#].[^\\s]*$"))
    }

    /**
     * Validate pagination parameters
     */
    fun isValidPagination(limit: Int, offset: Int): Boolean {
        return limit in 1..100 && offset >= 0
    }

    /**
     * Validate a date string (format: YYYY-MM-DD)
     */
    fun isValidDateString(date: String?): Boolean {
        if (date == null) return true // null is considered valid (no filter)
        return date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
    }

    /**
     * Validate an authentication type
     */
    fun isValidAuthType(authType: String): Boolean {
        return authType in listOf("oauth2", "login_password", "api_key")
    }

    /**
     * Validate webhook event type
     */
    fun isValidWebhookEvent(event: String): Boolean {
        return event in listOf(
            "transaction.created",
            "transaction.updated",
            "balance.changed",
            "account.created",
            "account.updated"
        )
    }
}