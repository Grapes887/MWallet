package com.example.mwallet.domain.di

import android.content.Context
import com.example.mwallet.data.auth.FirebaseAuthDataSource
import com.example.mwallet.data.local.TokenStorage
import com.example.mwallet.data.remote.WalletApi
import com.example.mwallet.data.repository.AuthRepositoryImpl
import com.example.mwallet.data.repository.WalletRepositoryImpl
import com.example.mwallet.domain.repository.AuthRepository
import com.example.mwallet.domain.repository.WalletRepository
import com.example.mwallet.domain.usecase.CompletePasswordResetUseCase
import com.example.mwallet.domain.usecase.CompleteRegistrationUseCase
import com.example.mwallet.domain.usecase.DepositUseCase
import com.example.mwallet.domain.usecase.GetTransactionsUseCase
import com.example.mwallet.domain.usecase.GetWalletUseCase
import com.example.mwallet.domain.usecase.LogoutUseCase
import com.example.mwallet.domain.usecase.LookupPasswordResetUseCase
import com.example.mwallet.domain.usecase.ObserveAuthUseCase
import com.example.mwallet.domain.usecase.SearchUsersUseCase
import com.example.mwallet.domain.usecase.SignInUseCase
import com.example.mwallet.domain.usecase.StartPasswordResetUseCase
import com.example.mwallet.domain.usecase.StartRegistrationUseCase
import com.example.mwallet.domain.usecase.TransferUseCase
import com.example.mwallet.domain.usecase.VerifyPasswordResetCodeUseCase
import com.example.mwallet.presentation.auth.AuthViewModel
import com.example.mwallet.presentation.wallet.WalletViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(context: Context) {
    private val tokenStorage = TokenStorage(context.applicationContext)
    private val walletApi = WalletApi { tokenStorage.getToken() }
    private val firebaseAuth = FirebaseAuthDataSource()

    private val _passwordResetOobCode = MutableStateFlow<String?>(null)
    val passwordResetOobCode: StateFlow<String?> = _passwordResetOobCode.asStateFlow()

    val authRepository: AuthRepository = AuthRepositoryImpl(walletApi, tokenStorage, firebaseAuth)
    val walletRepository: WalletRepository = WalletRepositoryImpl(walletApi) {
        tokenStorage.clear()
    }

    val signInUseCase = SignInUseCase(authRepository)
    val startRegistrationUseCase = StartRegistrationUseCase(authRepository)
    val completeRegistrationUseCase = CompleteRegistrationUseCase(authRepository)
    val lookupPasswordResetUseCase = LookupPasswordResetUseCase(authRepository)
    val startPasswordResetUseCase = StartPasswordResetUseCase(authRepository)
    val completePasswordResetUseCase = CompletePasswordResetUseCase(authRepository)
    val verifyPasswordResetCodeUseCase = VerifyPasswordResetCodeUseCase(authRepository)
    val logoutUseCase = LogoutUseCase(authRepository)
    val observeAuthUseCase = ObserveAuthUseCase(authRepository)
    val getWalletUseCase = GetWalletUseCase(walletRepository)
    val depositUseCase = DepositUseCase(walletRepository)
    val transferUseCase = TransferUseCase(walletRepository)
    val getTransactionsUseCase = GetTransactionsUseCase(walletRepository)
    val searchUsersUseCase = SearchUsersUseCase(walletRepository)

    fun setPasswordResetOobCode(oobCode: String?) {
        _passwordResetOobCode.value = oobCode
    }

    fun authViewModel() = AuthViewModel(
        signInUseCase,
        startRegistrationUseCase,
        completeRegistrationUseCase,
        lookupPasswordResetUseCase,
        startPasswordResetUseCase,
        completePasswordResetUseCase,
        verifyPasswordResetCodeUseCase,
        observeAuthUseCase
    )

    fun walletViewModel(onSessionExpired: () -> Unit = {}) = WalletViewModel(
        getWalletUseCase,
        depositUseCase,
        transferUseCase,
        getTransactionsUseCase,
        searchUsersUseCase,
        logoutUseCase,
        onSessionExpired
    )
}
