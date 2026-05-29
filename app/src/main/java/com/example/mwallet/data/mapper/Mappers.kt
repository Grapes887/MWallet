package com.example.mwallet.data.mapper

import com.example.mwallet.data.dto.AuthResponseDto
import com.example.mwallet.data.dto.ResetLookupResponseDto
import com.example.mwallet.data.dto.TransactionResponseDto
import com.example.mwallet.data.dto.UserSearchResponseDto
import com.example.mwallet.data.dto.WalletResponseDto
import com.example.mwallet.domain.model.ResetLookupResult
import com.example.mwallet.domain.model.Transaction
import com.example.mwallet.domain.model.TransactionType
import com.example.mwallet.domain.model.User
import com.example.mwallet.domain.model.UserSearchResult
import com.example.mwallet.domain.model.Wallet
import java.math.BigDecimal

fun AuthResponseDto.toUser() = User(
    id = userId,
    username = username,
    token = token
)

fun ResetLookupResponseDto.toDomain() = ResetLookupResult(
    email = email,
    phone = phone,
    maskedEmail = maskedEmail,
    maskedPhone = maskedPhone,
    canResetByEmail = canResetByEmail,
    canResetByPhone = canResetByPhone
)

fun WalletResponseDto.toWallet() = Wallet(
    id = id,
    balance = BigDecimal(balance),
    currency = currency,
    username = username
)

fun TransactionResponseDto.toTransaction() = Transaction(
    id = id,
    amount = BigDecimal(amount),
    type = TransactionType.valueOf(type),
    description = description,
    counterparty = counterparty,
    createdAt = createdAt
)

fun UserSearchResponseDto.toUserSearchResult() = UserSearchResult(
    id = id,
    username = username,
    phone = phone
)
