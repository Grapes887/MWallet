package com.example.mwallet.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResolveRequestDto(
    val login: String
)

@Serializable
data class LoginResolveResponseDto(
    val email: String,
    val username: String
)

@Serializable
data class ResetLookupRequestDto(
    val login: String
)

@Serializable
data class ResetLookupResponseDto(
    val email: String? = null,
    val phone: String? = null,
    val maskedEmail: String? = null,
    val maskedPhone: String? = null,
    val canResetByEmail: Boolean,
    val canResetByPhone: Boolean
)

@Serializable
data class FirebaseAuthRequestDto(
    val idToken: String,
    val nickname: String? = null,
    val phone: String? = null
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val userId: Long,
    val username: String,
    val profileComplete: Boolean = true
)

@Serializable
data class UserSearchResponseDto(
    val id: Long,
    val username: String,
    val phone: String?
)

@Serializable
data class WalletResponseDto(
    val id: Long,
    val balance: String,
    val currency: String,
    val username: String
)

@Serializable
data class DepositRequestDto(
    val amount: String
)

@Serializable
data class TransferRequestDto(
    val toUsername: String,
    val amount: String,
    val description: String? = null
)

@Serializable
data class TransactionResponseDto(
    val id: Long,
    val amount: String,
    val type: String,
    val description: String?,
    val counterparty: String?,
    val createdAt: String
)

@Serializable
data class ErrorResponseDto(
    val message: String
)
