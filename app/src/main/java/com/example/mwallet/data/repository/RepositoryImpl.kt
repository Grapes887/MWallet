package com.example.mwallet.data.repository

import android.app.Activity
import com.example.mwallet.data.auth.FirebaseAuthDataSource
import com.example.mwallet.data.dto.FirebaseAuthRequestDto
import com.example.mwallet.data.local.TokenStorage
import com.example.mwallet.data.mapper.toDomain
import com.example.mwallet.data.mapper.toTransaction
import com.example.mwallet.data.mapper.toUser
import com.example.mwallet.data.mapper.toUserSearchResult
import com.example.mwallet.data.mapper.toWallet
import com.example.mwallet.data.remote.ApiException
import com.example.mwallet.data.remote.WalletApi
import com.example.mwallet.domain.model.PendingRegistration
import com.example.mwallet.domain.model.ResetLookupResult
import com.example.mwallet.domain.model.Transaction
import com.example.mwallet.domain.model.User
import com.example.mwallet.domain.model.UserSearchResult
import com.example.mwallet.domain.model.Wallet
import com.example.mwallet.domain.repository.AuthRepository
import com.example.mwallet.domain.repository.WalletRepository
import com.example.mwallet.domain.usecase.normalizedPhone
import java.math.BigDecimal

class AuthRepositoryImpl(
    private val api: WalletApi,
    private val tokenStorage: TokenStorage,
    private val firebaseAuth: FirebaseAuthDataSource
) : AuthRepository {

    override suspend fun signIn(login: String, password: String): User {
        val resolved = api.resolveLogin(login)
        firebaseAuth.signInWithEmail(resolved.email, password)
        return syncWithBackend(nickname = null, phone = firebaseAuth.currentPhone())
    }

    override suspend fun startRegistration(activity: Activity, data: PendingRegistration): String {
        return firebaseAuth.sendPhoneCode(
            activity = activity,
            phoneNumber = data.normalizedPhone(),
            autoSignIn = false
        )
    }

    override suspend fun completeRegistration(
        verificationId: String,
        smsCode: String,
        data: PendingRegistration
    ): User {
        val phoneCredential = firebaseAuth.buildPhoneCredential(verificationId, smsCode)
        firebaseAuth.registerWithEmailAndLinkPhone(
            email = data.email.trim(),
            password = data.password,
            phoneCredential = phoneCredential
        )
        return syncWithBackend(
            nickname = data.nickname.trim(),
            phone = data.normalizedPhone()
        )
    }

    override suspend fun lookupPasswordReset(login: String): ResetLookupResult =
        api.lookupPasswordReset(login).toDomain()

    override suspend fun startPasswordResetByPhone(activity: Activity, phone: String): String {
        val normalized = phone.filter { it.isDigit() || it == '+' }.let {
            if (it.startsWith("+")) it else "+$it"
        }
        return firebaseAuth.sendPhoneCode(activity, normalized, autoSignIn = false)
    }

    override suspend fun completePasswordResetByPhone(
        verificationId: String,
        smsCode: String,
        newPassword: String
    ) {
        firebaseAuth.signInWithPhoneCredential(verificationId, smsCode)
        firebaseAuth.updatePassword(newPassword)
        firebaseAuth.signOut()
    }

    override suspend fun startPasswordResetByEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
    }

    override suspend fun completePasswordResetByEmail(oobCode: String, newPassword: String) {
        firebaseAuth.confirmPasswordReset(oobCode, newPassword)
    }

    override suspend fun verifyPasswordResetCode(oobCode: String): String =
        firebaseAuth.verifyPasswordResetCode(oobCode)

    override suspend fun logout() {
        firebaseAuth.signOut()
        tokenStorage.clear()
    }

    override suspend fun isLoggedIn(): Boolean = tokenStorage.getToken() != null

    override suspend fun clearSession() {
        tokenStorage.clear()
    }

    private suspend fun syncWithBackend(nickname: String?, phone: String?): User {
        val idToken = firebaseAuth.getIdToken()
        val response = api.authenticateWithFirebase(
            FirebaseAuthRequestDto(
                idToken = idToken,
                nickname = nickname,
                phone = phone
            )
        )
        val user = response.toUser()
        tokenStorage.saveUser(user)
        return user
    }
}

class WalletRepositoryImpl(
    private val api: WalletApi,
    private val onUnauthorized: suspend () -> Unit = {}
) : WalletRepository {

    override suspend fun getWallet(): Wallet = withAuth { api.getWallet().toWallet() }

    override suspend fun deposit(amount: BigDecimal): Wallet =
        withAuth { api.deposit(amount.toPlainString()).toWallet() }

    override suspend fun transfer(toUsername: String, amount: BigDecimal, description: String?): Wallet =
        withAuth { api.transfer(toUsername, amount.toPlainString(), description).toWallet() }

    override suspend fun getTransactions(): List<Transaction> =
        withAuth { api.getTransactions().map { it.toTransaction() } }

    override suspend fun searchUsers(query: String): List<UserSearchResult> =
        withAuth { api.searchUsers(query).map { it.toUserSearchResult() } }

    private suspend fun <T> withAuth(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: ApiException) {
            if (e.code == 401) onUnauthorized()
            throw e
        }
    }
}
