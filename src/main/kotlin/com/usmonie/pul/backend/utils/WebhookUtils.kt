package com.usmonie.pul.backend.utils

import org.slf4j.LoggerFactory
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for webhook signature generation and validation
 */
object WebhookUtils {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Generate HMAC-SHA256 signature for webhook payload
     *
     * @param payload The webhook payload to sign
     * @param secretKey The secret key to use for signing
     * @return The HMAC-SHA256 signature as a hex string
     */
    fun generateSignature(payload: String, secretKey: String): String {
        try {
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKeySpec)
            val signature = mac.doFinal(payload.toByteArray())
            return signature.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("Error generating signature: ${e.message}")
            throw RuntimeException("Failed to generate webhook signature", e)
        }
    }

    /**
     * Validate a webhook signature
     *
     * @param payload The webhook payload
     * @param secretKey The secret key used for signing
     * @param signature The signature to validate
     * @return true if the signature is valid, false otherwise
     */
    fun validateSignature(payload: String, secretKey: String, signature: String): Boolean {
        try {
            val calculatedSignature = generateSignature(payload, secretKey)
            return calculatedSignature == signature
        } catch (e: Exception) {
            logger.error("Error validating signature: ${e.message}")
            return false
        }
    }
}