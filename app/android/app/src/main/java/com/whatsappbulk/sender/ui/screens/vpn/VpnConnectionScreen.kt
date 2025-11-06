package com.whatsappbulk.sender.ui.screens.vpn

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsappbulk.sender.R
import kotlinx.coroutines.launch

@Composable
fun VpnConnectionScreen(
    onNavigateToLogin: () -> Unit,
    onRequireInstall: () -> Unit,
    viewModel: VpnConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Back: prevent navigating back; allow double back to exit
    val backPressedOnce = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(true) {
        if (backPressedOnce.value) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce.value = true
            Toast.makeText(context, context.getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
            scope.launch {
                kotlinx.coroutines.delay(2000)
                backPressedOnce.value = false
            }
        }
    }

    // onResume: check if FortiClient installed; if not, send to install
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshFortiInstalled()
                if (!viewModel.uiState.value.isFortiClientInstalled) {
                    onRequireInstall()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    Scaffold { padding ->
        Content(paddingValues = padding,
            isChecking = uiState.isChecking,
            isInstalled = uiState.isFortiClientInstalled,
            errorMessage = uiState.errorMessage,
            onOpenForti = { viewModel.openFortiClient() },
            onVerify = { viewModel.verifyConnection(onConnected = onNavigateToLogin) }
        )
    }
}

@Composable
private fun Content(
    paddingValues: PaddingValues,
    isChecking: Boolean,
    isInstalled: Boolean,
    errorMessage: String?,
    onOpenForti: () -> Unit,
    onVerify: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(id = R.string.vpn_connection_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(id = R.string.vpn_connection_message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        if (!isInstalled) {
            Text(text = stringResource(id = R.string.vpn_connection_not_installed), color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = isInstalled && !isChecking,
            onClick = onOpenForti,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text(text = stringResource(id = R.string.vpn_open_forticlient)) }

        Button(
            enabled = !isChecking,
            onClick = onVerify,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            if (isChecking) CircularProgressIndicator() else Text(text = stringResource(id = R.string.vpn_verify_connection))
        }

        if (errorMessage != null) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
