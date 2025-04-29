package com.usmonie.pul.backend.models

import kotlinx.serialization.*

@Serializable
data class Transaction(
    val transactionId: String,
    val date: String,
    val amount: Double,
    val currency: String,
    val description: String,
    val merchant: String? = null,
    val category: String? = null,
    val status: String? = null
)
