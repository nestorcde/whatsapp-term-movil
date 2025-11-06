package com.whatsappbulk.sender.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsappbulk.sender.R
import androidx.compose.ui.res.stringResource

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToVpnConnection: () -> Unit,
    onNavigateToVpnInstall: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.runHealthCheck { isConnected, fortiInstalled ->
            if (isConnected) {
                onNavigateToLogin()
            } else {
                if (fortiInstalled) onNavigateToVpnConnection() else onNavigateToVpnInstall()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (uiState.isChecking) {
            CircularProgressIndicator()
        } else {
            Text(text = stringResource(id = R.string.splash_checking_connection), color = MaterialTheme.colorScheme.primary)
        }
    }
}
