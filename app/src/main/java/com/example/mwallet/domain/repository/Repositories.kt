package com.example.mwallet.domain.repository

import android.app.Activity
import com.example.mwallet.domain.model.PendingRegistration
import com.example.mwallet.domain.model.ResetLookupResult
import com.example.mwallet.domain.model.ResetMethod
import com.example.mwallet.domain.model.Transaction
import com.example.mwallet.domain.model.User
import com.example.mwallet.domain.model.UserSearchResult
import com.example.mwallet.domain.model.Wallet
import java.math.BigDecimal

interface AuthRepository {
    suspend fun signIn(login: String, password: String): User
    suspend fun startRegistration(activity: Activity, data: PendingRegistration): String
    suspend fun completeRegistration(verificationId: String, smsCode: String, data: PendingRegistration): User
    suspend fun lookupPasswordReset(login: String): ResetLookupResult
    suspend fun startPasswordResetByPhone(activity: Activity, phone: String): String
    suspend fun completePasswordResetByPhone(verificationId: String, smsCode: String, newPassword: String)
    suspend fun startPasswordResetByEmail(email: String)
    suspend fun verifyPasswordResetCode(oobCode: String): String
    suspend fun completePasswordResetByEmail(oobCode: String, newPassword: String)
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun clearSession()
}

interface WalletRepository {
    suspend fun getWallet(): Wallet
    suspend fun deposit(amount: BigDecimal): Wallet
    suspend fun transfer(toUsername: String, amount: BigDecimal, description: String?): Wallet
    suspend fun getTransactions(): List<Transaction>
    suspend fun searchUsers(query: String): List<UserSearchResult>
}
