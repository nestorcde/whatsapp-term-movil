package com.whatsappbulk.sender.ui.screens.vpn

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.whatsappbulk.sender.R
import kotlinx.coroutines.launch

@Composable
fun VpnInstallScreen(
    onInstallAndNavigate: () -> Unit,
    viewModel: VpnInstallViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Double back to exit only (no back navigation)
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

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            if (event is NavigationEvent.ToVpnConnection) {
                onInstallAndNavigate()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(id = R.string.vpn_install_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(id = R.string.vpn_install_message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        Text(text = stringResource(id = R.string.vpn_install_instructions))
        Button(onClick = { viewModel.onInstallClick() }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(text = stringResource(id = R.string.vpn_install_button))
        }
    }
}
