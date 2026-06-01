package com.example.mwallet.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mwallet.domain.di.AppContainer
import com.example.mwallet.presentation.auth.AuthFlowScreen
import com.example.mwallet.presentation.wallet.WalletScreen

object Routes {
    const val AUTH = "auth"
    const val WALLET = "wallet"
}

@Composable
fun MwalletNavHost(appContainer: AppContainer) {
    val navController = rememberNavController()
    val authViewModel = viewModel { appContainer.authViewModel() }
    val authState by authViewModel.uiState.collectAsState()
    val pendingOobCode by appContainer.passwordResetOobCode.collectAsState()

    LaunchedEffect(pendingOobCode) {
        pendingOobCode?.let { oobCode ->
            authViewModel.openPasswordResetFromLink(oobCode)
            appContainer.setPasswordResetOobCode(null)
            navController.navigate(Routes.AUTH) {
                popUpTo(Routes.WALLET) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val startDestination = if (authState.isAuthenticated) Routes.WALLET else Routes.AUTH

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.AUTH) {
            AuthFlowScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Routes.WALLET) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.WALLET) {
            val navigateToAuth = {
                authViewModel.resetToLogin()
                navController.navigate(Routes.AUTH) {
                    popUpTo(Routes.WALLET) { inclusive = true }
                }
            }
            val walletViewModel = viewModel {
                appContainer.walletViewModel(onSessionExpired = navigateToAuth)
            }
            WalletScreen(
                viewModel = walletViewModel,
                onLogout = navigateToAuth
            )
        }
    }
}
