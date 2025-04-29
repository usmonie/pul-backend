package com.usmonie.pul.backend.models

import kotlinx.serialization.*

@Serializable
data class ErrorResponse(
    val error: String,
    val errorDescription: String,
    val details: Map<String, String> = emptyMap()
)
