package com.example.mwallet.presentation.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mwallet.domain.model.PendingRegistration
import com.example.mwallet.domain.model.ResetLookupResult
import com.example.mwallet.domain.model.ResetMethod
import com.example.mwallet.domain.usecase.CompletePasswordResetUseCase
import com.example.mwallet.domain.usecase.CompleteRegistrationUseCase
import com.example.mwallet.domain.usecase.LookupPasswordResetUseCase
import com.example.mwallet.domain.usecase.ObserveAuthUseCase
import com.example.mwallet.domain.usecase.SignInUseCase
import com.example.mwallet.domain.usecase.StartPasswordResetUseCase
import com.example.mwallet.domain.usecase.StartRegistrationUseCase
import com.example.mwallet.domain.usecase.VerifyPasswordResetCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    LOGIN,
    REGISTER,
    RESET_PASSWORD
}

enum class RegisterStep {
    FORM,
    SMS_CODE
}

enum class ResetStep {
    IDENTIFIER,
    CHOOSE_METHOD,
    ENTER_CODE,
    EMAIL_LINK_SENT,
    NEW_PASSWORD_FROM_LINK
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val screenMode: AuthScreenMode = AuthScreenMode.LOGIN,
    val registerStep: RegisterStep = RegisterStep.FORM,
    val resetStep: ResetStep = ResetStep.IDENTIFIER,
    val pendingRegistration: PendingRegistration? = null,
    val phoneVerificationId: String? = null,
    val resetLookup: ResetLookupResult? = null,
    val resetMethod: ResetMethod? = null,
    val emailResetOobCode: String? = null,
    val verifiedResetEmail: String? = null
)

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val startRegistrationUseCase: StartRegistrationUseCase,
    private val completeRegistrationUseCase: CompleteRegistrationUseCase,
    private val lookupPasswordResetUseCase: LookupPasswordResetUseCase,
    private val startPasswordResetUseCase: StartPasswordResetUseCase,
    private val completePasswordResetUseCase: CompletePasswordResetUseCase,
    private val verifyPasswordResetCodeUseCase: VerifyPasswordResetCodeUseCase,
    private val observeAuthUseCase: ObserveAuthUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loggedIn = observeAuthUseCase()
            _uiState.update { it.copy(isAuthenticated = loggedIn) }
        }
    }

    fun resetToLogin() {
        _uiState.value = AuthUiState(isAuthenticated = false)
    }

    fun showLogin() {
        _uiState.update {
            AuthUiState(
                isAuthenticated = it.isAuthenticated,
                screenMode = AuthScreenMode.LOGIN
            )
        }
    }

    fun showRegister() {
        _uiState.update {
            it.copy(
                screenMode = AuthScreenMode.REGISTER,
                registerStep = RegisterStep.FORM,
                pendingRegistration = null,
                phoneVerificationId = null,
                error = null
            )
        }
    }

    fun showResetPassword() {
        _uiState.update {
            it.copy(
                screenMode = AuthScreenMode.RESET_PASSWORD,
                resetStep = ResetStep.IDENTIFIER,
                resetLookup = null,
                resetMethod = null,
                phoneVerificationId = null,
                error = null
            )
        }
    }

    fun signIn(login: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { signInUseCase(login, password) }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                    onSuccess()
                }
                .onFailure { e ->
                    val message = when
                    {
                        e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                            "Неверный пароль"
                        else ->
                            e.message
                    }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    fun startRegistration(
        activity: Activity,
        nickname: String,
        email: String,
        password: String,
        phone: String
    ) {
        viewModelScope.launch {
            val data = PendingRegistration(nickname, email, password, phone)
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { startRegistrationUseCase(activity, data) }
                .onSuccess { verificationId ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            registerStep = RegisterStep.SMS_CODE,
                            pendingRegistration = data,
                            phoneVerificationId = verificationId
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun completeRegistration(smsCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val data = _uiState.value.pendingRegistration ?: return@launch
            val verificationId = _uiState.value.phoneVerificationId.orEmpty()
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { completeRegistrationUseCase(verificationId, smsCode, data) }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun lookupResetAccount(login: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { lookupPasswordResetUseCase(login) }
                .onSuccess { lookup ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            resetLookup = lookup,
                            resetStep = when {
                                lookup.canResetByPhone && lookup.canResetByEmail -> ResetStep.CHOOSE_METHOD
                                lookup.canResetByPhone -> ResetStep.ENTER_CODE
                                lookup.canResetByEmail -> ResetStep.EMAIL_LINK_SENT
                                else -> ResetStep.IDENTIFIER
                            },
                            resetMethod = when {
                                lookup.canResetByPhone && !lookup.canResetByEmail -> ResetMethod.PHONE
                                lookup.canResetByEmail && !lookup.canResetByPhone -> ResetMethod.EMAIL
                                else -> null
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun chooseResetMethod(method: ResetMethod) {
        _uiState.update {
            it.copy(
                resetMethod = method,
                resetStep = when (method) {
                    ResetMethod.PHONE -> ResetStep.ENTER_CODE
                    ResetMethod.EMAIL -> ResetStep.EMAIL_LINK_SENT
                },
                error = null
            )
        }
    }

    fun startPasswordReset(activity: Activity) {
        viewModelScope.launch {
            val lookup = _uiState.value.resetLookup ?: return@launch
            val method = _uiState.value.resetMethod ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                startPasswordResetUseCase(activity, method, lookup.email, lookup.phone)
            }.onSuccess { verificationId ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phoneVerificationId = verificationId,
                        resetStep = if (method == ResetMethod.EMAIL) {
                            ResetStep.EMAIL_LINK_SENT
                        } else {
                            it.resetStep
                        },
                        successMessage = if (method == ResetMethod.EMAIL) {
                            "Откройте ссылку в письме на ${lookup.maskedEmail}"
                        } else {
                            "Код отправлен на ${lookup.maskedPhone}"
                        }
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun openPasswordResetFromLink(oobCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isAuthenticated = false) }
            runCatching { verifyPasswordResetCodeUseCase(oobCode) }
                .onSuccess { email ->
                    _uiState.value = AuthUiState(
                        screenMode = AuthScreenMode.RESET_PASSWORD,
                        resetStep = ResetStep.NEW_PASSWORD_FROM_LINK,
                        emailResetOobCode = oobCode,
                        verifiedResetEmail = email,
                        resetMethod = ResetMethod.EMAIL
                    )
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState(
                        error = e.message ?: "Ссылка недействительна или устарела",
                        screenMode = AuthScreenMode.LOGIN
                    )
                }
        }
    }

    fun completePasswordResetFromLink(newPassword: String, onSuccess: () -> Unit) {
        val oobCode = _uiState.value.emailResetOobCode ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                completePasswordResetUseCase(
                    method = ResetMethod.EMAIL,
                    verificationId = null,
                    code = oobCode,
                    newPassword = newPassword
                )
            }.onSuccess {
                _uiState.value = AuthUiState(
                    isAuthenticated = false,
                    successMessage = "Пароль изменён. Войдите с новым паролем",
                    screenMode = AuthScreenMode.LOGIN
                )
                onSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun completePasswordReset(code: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val method = _uiState.value.resetMethod ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            runCatching {
                completePasswordResetUseCase(
                    method = method,
                    verificationId = _uiState.value.phoneVerificationId,
                    code = code,
                    newPassword = newPassword
                )
            }.onSuccess {
                _uiState.value = AuthUiState(
                    isAuthenticated = false,
                    successMessage = "Пароль изменён. Войдите с новым паролем",
                    screenMode = AuthScreenMode.LOGIN
                )
                onSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
