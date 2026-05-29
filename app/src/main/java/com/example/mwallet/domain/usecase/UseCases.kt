package com.example.mwallet.domain.usecase

import android.app.Activity
import com.example.mwallet.domain.model.PendingRegistration
import com.example.mwallet.domain.model.ResetMethod
import com.example.mwallet.domain.model.Transaction
import com.example.mwallet.domain.model.User
import com.example.mwallet.domain.model.UserSearchResult
import com.example.mwallet.domain.model.Wallet
import com.example.mwallet.domain.repository.AuthRepository
import com.example.mwallet.domain.repository.WalletRepository
import java.math.BigDecimal

class SignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(login: String, password: String): User {
        require(login.isNotBlank()) { "Введите логин, email или телефон" }
        require(password.length >= 6) { "Пароль — минимум 6 символов" }
        return authRepository.signIn(login.trim(), password)
    }
}

class StartRegistrationUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(activity: Activity, data: PendingRegistration): String {
        require(data.nickname.length >= 3) { "Никнейм — минимум 3 символа" }
        require(data.email.contains("@")) { "Некорректный email" }
        require(data.password.length >= 6) { "Пароль — минимум 6 символов" }
        require(data.phone.filter { it.isDigit() }.length >= 10) { "Введите корректный телефон" }
        return authRepository.startRegistration(activity, data)
    }
}

class CompleteRegistrationUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        verificationId: String,
        smsCode: String,
        data: PendingRegistration
    ): User {
        require(smsCode.length >= 6) { "Введите код из SMS" }
        return authRepository.completeRegistration(verificationId, smsCode, data)
    }
}

class LookupPasswordResetUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(login: String) = authRepository.lookupPasswordReset(login.trim())
}

class StartPasswordResetUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        activity: Activity,
        method: ResetMethod,
        email: String?,
        phone: String?
    ): String? {
        return when (method) {
            ResetMethod.PHONE -> {
                val p = phone ?: error("Телефон не указан")
                authRepository.startPasswordResetByPhone(activity, p)
            }
            ResetMethod.EMAIL -> {
                val e = email ?: error("Email не указан")
                authRepository.startPasswordResetByEmail(e)
                null
            }
        }
    }
}

class VerifyPasswordResetCodeUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(oobCode: String): String =
        authRepository.verifyPasswordResetCode(oobCode)
}

class CompletePasswordResetUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        method: ResetMethod,
        verificationId: String?,
        code: String,
        newPassword: String
    ) {
        require(newPassword.length >= 6) { "Пароль — минимум 6 символов" }
        when (method) {
            ResetMethod.PHONE -> authRepository.completePasswordResetByPhone(
                verificationId.orEmpty(),
                code,
                newPassword
            )
            ResetMethod.EMAIL -> authRepository.completePasswordResetByEmail(code, newPassword)
        }
    }
}

class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke() = authRepository.logout()
}

class ObserveAuthUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Boolean = authRepository.isLoggedIn()
}

class GetWalletUseCase(private val walletRepository: WalletRepository) {
    suspend operator fun invoke(): Wallet = walletRepository.getWallet()
}

class DepositUseCase(private val walletRepository: WalletRepository) {
    suspend operator fun invoke(amount: BigDecimal): Wallet {
        require(amount > BigDecimal.ZERO) { "Сумма должна быть больше нуля" }
        return walletRepository.deposit(amount)
    }
}

class TransferUseCase(private val walletRepository: WalletRepository) {
    suspend operator fun invoke(toUsername: String, amount: BigDecimal, description: String?): Wallet {
        require(amount > BigDecimal.ZERO) { "Сумма должна быть больше нуля" }
        require(toUsername.isNotBlank()) { "Выберите получателя" }
        return walletRepository.transfer(toUsername.trim(), amount, description?.trim())
    }
}

class GetTransactionsUseCase(private val walletRepository: WalletRepository) {
    suspend operator fun invoke(): List<Transaction> = walletRepository.getTransactions()
}

class SearchUsersUseCase(private val walletRepository: WalletRepository) {
    suspend operator fun invoke(query: String): List<UserSearchResult> {
        require(query.trim().length >= 2) { "Введите минимум 2 символа" }
        return walletRepository.searchUsers(query.trim())
    }
}

private fun normalizePhone(phone: String): String =
    phone.filter { it.isDigit() || it == '+' }.let { if (it.startsWith("+")) it else "+$it" }

fun PendingRegistration.normalizedPhone() = normalizePhone(phone)
