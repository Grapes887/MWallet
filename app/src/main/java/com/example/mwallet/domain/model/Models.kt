package com.example.mwallet.domain.model

import java.math.BigDecimal

data class User(
    val id: Long,
    val username: String,
    val token: String
)

data class UserSearchResult(
    val id: Long,
    val username: String,
    val phone: String?
)

data class Wallet(
    val id: Long,
    val balance: BigDecimal,
    val currency: String,
    val username: String
)

enum class TransactionType {
    DEPOSIT,
    TRANSFER
}

data class Transaction(
    val id: Long,
    val amount: BigDecimal,
    val type: TransactionType,
    val description: String?,
    val counterparty: String?,
    val createdAt: String
)
