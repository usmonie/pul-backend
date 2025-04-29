package com.usmonie.pul.backend.security

import com.usmonie.pul.backend.models.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A plugin for implementing rate limiting in Ktor.
 * This limits the number of requests a client can make within a specific time window.
 */
class RateLimit(private val configuration: Configuration) {

    /**
     * Configuration options for the RateLimit plugin.
     */
    class Configuration {
        /** Maximum number of requests allowed in the time window */
        var limit: Int = 100

        /** Time window in minutes for counting requests */
        var timeWindowMinutes: Int = 1

        /** Function to extract a key to identify clients for rate limiting */
        var keyExtractor: (ApplicationCall) -> String = { call ->
            call.request.header("Authorization")?.substringAfter("Bearer ")
                ?: getClientIp(call.request)
        }
    }

    /**
     * Plugin companion object for installation in Ktor.
     */
    companion object Plugin : BaseApplicationPlugin<Application, Configuration, RateLimit> {
        override val key = AttributeKey<RateLimit>("RateLimit")

        // Store for request timestamps
        private val requestTimestamps = ConcurrentHashMap<String, CopyOnWriteArrayList<Long>>()

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RateLimit {
            // Create and configure plugin
            val configuration = Configuration().apply(configure)
            val plugin = RateLimit(configuration)

            // Add interceptor to check rate limits
            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                val clientKey = configuration.keyExtractor(call)
                val now = System.currentTimeMillis()
                val windowStartTime = now - (configuration.timeWindowMinutes * 60 * 1000)

                // Get or create timestamps list for this client
                val timestamps = requestTimestamps.getOrPut(clientKey) { CopyOnWriteArrayList() }

                // Remove timestamps outside the current time window
                timestamps.removeIf { it < windowStartTime }

                // Check if client has exceeded the rate limit
                if (timestamps.size >= configuration.limit) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ErrorResponse(
                            error = "rate_limit_exceeded",
                            errorDescription = "Слишком много запросов. Лимит: ${configuration.limit} запросов за ${configuration.timeWindowMinutes} минут."
                        )
                    )
                    finish()
                    return@intercept
                }

                // Add current timestamp to the list
                timestamps.add(now)
            }

            return plugin
        }
    }
}

/**
 * Gets the client IP address from a request, handling proxies via X-Forwarded-For.
 *
 * @param request The application request
 * @return The client's IP address as a string
 */
private fun getClientIp(request: ApplicationRequest): String {
    // Try to get from X-Forwarded-For header first (for proxied requests)
    request.header("X-Forwarded-For")?.let {
        val addresses = it.split(",").map { addr -> addr.trim() }
        if (addresses.isNotEmpty()) {
            return addresses.first()
        }
    }

    // Fall back to the direct connection address
    return request.local.remoteAddress
}