package com.whatsappbulk.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsappbulk.sender.ui.screens.login.LoginScreen
import com.whatsappbulk.sender.ui.screens.login.LoginViewModel
import com.whatsappbulk.sender.ui.screens.whatsapp.WhatsAppSessionScreen
import com.whatsappbulk.sender.ui.theme.WhatsAppBulkSenderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhatsAppBulkSenderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("login") }

                    when (currentScreen) {
                        "login" -> {
                            val loginViewModel: LoginViewModel = hiltViewModel()
                            val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

                            LoginScreen(
                                uiState = uiState,
                                onUsernameChange = loginViewModel::onUsernameChange,
                                onPasswordChange = loginViewModel::onPasswordChange,
                                onLoginClick = loginViewModel::login,
                                onNavigateToWhatsApp = {
                                    currentScreen = "whatsapp"
                                }
                            )
                        }
                        "whatsapp" -> {
                            WhatsAppSessionScreen()
                        }
                    }
                }
            }
        }
    }
}