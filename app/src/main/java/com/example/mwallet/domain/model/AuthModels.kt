package com.example.mwallet.domain.model

data class ResetLookupResult(
    val email: String?,
    val phone: String?,
    val maskedEmail: String?,
    val maskedPhone: String?,
    val canResetByEmail: Boolean,
    val canResetByPhone: Boolean
)

enum class ResetMethod {
    PHONE,
    EMAIL
}

data class PendingRegistration(
    val nickname: String,
    val email: String,
    val password: String,
    val phone: String
)
