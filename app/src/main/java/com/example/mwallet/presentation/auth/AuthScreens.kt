package com.example.mwallet.presentation.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mwallet.domain.model.ResetMethod

@Composable
fun AuthFlowScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as Activity

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mwallet", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))

            when (uiState.screenMode) {
                AuthScreenMode.LOGIN -> LoginForm(
                    isLoading = uiState.isLoading,
                    onSignIn = { login, password -> viewModel.signIn(login, password, onAuthSuccess) },
                    onRegister = { viewModel.showRegister() },
                    onForgotPassword = { viewModel.showResetPassword() }
                )

                AuthScreenMode.REGISTER -> when (uiState.registerStep) {
                    RegisterStep.FORM -> RegisterForm(
                        isLoading = uiState.isLoading,
                        onSubmit = { nickname, email, password, phone ->
                            viewModel.startRegistration(activity, nickname, email, password, phone)
                        },
                        onBackToLogin = { viewModel.showLogin() }
                    )
                    RegisterStep.SMS_CODE -> SmsCodeForm(
                        title = "Подтверждение регистрации",
                        subtitle = "Введите код из SMS, отправленный на указанный номер",
                        isLoading = uiState.isLoading,
                        showNewPassword = false,
                        onSubmit = { code, _ -> viewModel.completeRegistration(code, onAuthSuccess) },
                        onBack = { viewModel.showRegister() }
                    )
                }

                AuthScreenMode.RESET_PASSWORD -> ResetPasswordFlow(
                    uiState = uiState,
                    activity = activity,
                    viewModel = viewModel,
                    onBackToLogin = { viewModel.showLogin() }
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    isLoading: Boolean,
    onSignIn: (String, String) -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text("Вход", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = login,
        onValueChange = { login = it },
        label = { Text("Логин, email или телефон") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Пароль") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation()
    )
    Spacer(Modifier.height(24.dp))

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Button(onClick = { onSignIn(login, password) }, modifier = Modifier.fillMaxWidth()) {
            Text("Войти")
        }
        TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
            Text("Забыли пароль?")
        }
        TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Создать аккаунт")
        }
    }
}

@Composable
private fun RegisterForm(
    isLoading: Boolean,
    onSubmit: (String, String, String, String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text("Регистрация", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = nickname,
        onValueChange = { nickname = it },
        label = { Text("Никнейм") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Телефон (+7...)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Пароль") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation()
    )
    Spacer(Modifier.height(24.dp))

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Button(
            onClick = { onSubmit(nickname, email, password, phone) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Продолжить")
        }
        TextButton(onClick = onBackToLogin) {
            Text("Уже есть аккаунт")
        }
    }
}

@Composable
private fun ResetPasswordFlow(
    uiState: AuthUiState,
    activity: Activity,
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    when (uiState.resetStep) {
        ResetStep.IDENTIFIER -> {
            var login by remember { mutableStateOf("") }
            Text("Восстановление пароля", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Логин, email или телефон") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.lookupResetAccount(login) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Продолжить")
                }
                TextButton(onClick = onBackToLogin) {
                    Text("Назад ко входу")
                }
            }
        }

        ResetStep.CHOOSE_METHOD -> {
            val lookup = uiState.resetLookup ?: return
            Text("Куда отправить код?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            if (lookup.canResetByPhone) {
                OutlinedButton(
                    onClick = {
                        viewModel.chooseResetMethod(ResetMethod.PHONE)
                        viewModel.startPasswordReset(activity)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SMS на ${lookup.maskedPhone}")
                }
                Spacer(Modifier.height(8.dp))
            }
            if (lookup.canResetByEmail) {
                OutlinedButton(
                    onClick = {
                        viewModel.chooseResetMethod(ResetMethod.EMAIL)
                        viewModel.startPasswordReset(activity)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ссылка на ${lookup.maskedEmail}")
                }
            }
            TextButton(onClick = onBackToLogin) {
                Text("Назад")
            }
        }

        ResetStep.ENTER_CODE -> {
            val lookup = uiState.resetLookup
            if (uiState.phoneVerificationId == null && !uiState.isLoading) {
                LaunchedEffect(Unit) {
                    viewModel.startPasswordReset(activity)
                }
            }
            SmsCodeForm(
                title = "Новый пароль",
                subtitle = "Код отправлен на ${lookup?.maskedPhone}",
                isLoading = uiState.isLoading,
                showNewPassword = true,
                onSubmit = { code, newPassword ->
                    viewModel.completePasswordReset(code, newPassword) {}
                },
                onBack = onBackToLogin
            )
        }

        ResetStep.EMAIL_LINK_SENT -> {
            val lookup = uiState.resetLookup
            if (uiState.successMessage == null && !uiState.isLoading) {
                LaunchedEffect(Unit) {
                    viewModel.startPasswordReset(activity)
                }
            }
            Text("Проверьте почту", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.successMessage
                    ?: "Отправляем письмо на ${lookup?.maskedEmail}...",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Перейдите по ссылке в письме — приложение откроется, и вы сможете задать новый пароль.",
                style = MaterialTheme.typography.bodySmall
            )
            if (uiState.isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onBackToLogin) {
                Text("Назад ко входу")
            }
        }

        ResetStep.NEW_PASSWORD_FROM_LINK -> {
            var newPassword by remember { mutableStateOf("") }
            Text("Новый пароль", style = MaterialTheme.typography.titleLarge)
            uiState.verifiedResetEmail?.let { email ->
                Text(
                    text = "Email подтверждён: $email",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Новый пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(24.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        viewModel.completePasswordResetFromLink(newPassword) {
                            onBackToLogin()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить пароль")
                }
                TextButton(onClick = onBackToLogin) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
private fun SmsCodeForm(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    showNewPassword: Boolean,
    codeLabel: String = "Код из SMS",
    onSubmit: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text(codeLabel) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    if (showNewPassword) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Новый пароль") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
    }
    Spacer(Modifier.height(24.dp))

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Button(
            onClick = { onSubmit(code, newPassword) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showNewPassword) "Сохранить пароль" else "Подтвердить")
        }
        TextButton(onClick = onBack) {
            Text("Назад")
        }
    }
}
