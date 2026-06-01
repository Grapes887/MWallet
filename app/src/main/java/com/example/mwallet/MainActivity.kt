package com.example.mwallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mwallet.presentation.navigation.MwalletNavHost
import com.example.mwallet.ui.theme.MwalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handlePasswordResetDeepLink(intent)
        val appContainer = (application as MwalletApplication).appContainer
        setContent {
            MwalletTheme {
                MwalletNavHost(appContainer)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePasswordResetDeepLink(intent)
    }

    private fun handlePasswordResetDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        extractPasswordResetCode(uri)?.let { oobCode ->
            (application as MwalletApplication).appContainer.setPasswordResetOobCode(oobCode)
        }
    }

    companion object {
        fun extractPasswordResetCode(uri: Uri): String? {
            if (uri.getQueryParameter("mode") != "resetPassword") return null
            return uri.getQueryParameter("oobCode")?.takeIf { it.isNotBlank() }
        }
    }
}
