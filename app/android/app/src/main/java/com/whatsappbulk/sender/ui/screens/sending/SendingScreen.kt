package com.whatsappbulk.sender.ui.screens.sending

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.whatsappbulk.sender.work.SendingWorker
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendingScreen(
    onBack: () -> Unit,
    viewModel: SendingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviando") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.start() }) { Text("Reintentar") }
                }
            } else {
                Content(
                    state = uiState,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onCancel = viewModel::cancel,
                    onBackground = {
                        val cid = uiState.campaignId ?: return@Content
                        val qty = uiState.pending
                        if (qty <= 0) return@Content
                        val data = Data.Builder()
                            .putInt(SendingWorker.KEY_CAMPAIGN_ID, cid)
                            .putInt(SendingWorker.KEY_QUANTITY, qty)
                            .build()
                        val req = OneTimeWorkRequestBuilder<SendingWorker>()
                            .setInputData(data)
                            .build()
                        // Cancelar envío en primer plano y encolar background
                        viewModel.cancel()
                        WorkManager.getInstance(context).enqueue(req)
                        // Opcional: navegar atrás
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun Content(
    state: SendingUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onBackground: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card { Column(Modifier.padding(16.dp)) {
                Text("Campaña: ${state.campaignName}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (state.total == 0) 0f else state.sent.toFloat() / state.total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text("${state.sent} / ${state.total} enviados")
            }}
        }

        item {
            Card { Column(Modifier.padding(16.dp)) {
                Text("Estadísticas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Enviados: ${state.sent}")
                Text("Pendientes: ${state.pending}")
                Text("Fallidos: ${state.failed}")
            }}
        }

        state.currentContact?.let { cc ->
            item {
                Card { Column(Modifier.padding(16.dp)) {
                    Text("Enviando ahora:")
                    Text(cc, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    state.currentMessage?.let { msg -> Text(msg) }
                }}
            }
        }

        item {
            if (!state.isCompleted && !state.isCancelled && state.pending > 0) {
                Text("Próximo en: ${state.nextInSeconds}s")
            } else if (state.isCompleted) {
                Text("Completado", color = MaterialTheme.colorScheme.primary)
            } else if (state.isCancelled) {
                Text("Cancelado", color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!state.isPaused) {
                    Button(onClick = onPause, enabled = !state.isCompleted && !state.isCancelled) { Text("Pausar") }
                } else {
                    Button(onClick = onResume, enabled = !state.isCompleted && !state.isCancelled) { Text("Reanudar") }
                }
                OutlinedButton(onClick = onCancel, enabled = !state.isCompleted && !state.isCancelled) { Text("Cancelar") }
                OutlinedButton(onClick = onBackground, enabled = !state.isCompleted && !state.isCancelled) { Text("Segundo plano") }
            }
        }
    }
}
