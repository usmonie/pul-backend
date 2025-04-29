package com.usmonie.pul.backend.models

import kotlinx.serialization.*

@Serializable
data class Account(
    val accountId: String,
    val type: String,
    val currency: String,
    val maskedNumber: String,
    val balance: Double,
    val accountName: String? = null
)

@Serializable
data class AccountBalance(
    val accountId: String,
    val balance: Double,
    val available: Double,
    val currency: String,
    val asOf: Long
)
