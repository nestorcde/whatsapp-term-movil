package com.whatsappbulk.sender.ui.screens.campaigns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsappbulk.sender.domain.model.CampaignFull
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.whatsappbulk.sender.work.SendingWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailsScreen(
    onBack: () -> Unit,
    onStartSend: (campaignId: Int, quantity: Int) -> Unit,
    viewModel: CampaignDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles de campa??a") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.load() }) { Text("Reintentar") }
                    }
                }
                uiState.campaign != null -> {
                    DetailsContent(
                        full = uiState.campaign!!,
                        quantityText = uiState.quantityText,
                        maxQuantity = uiState.maxQuantity,
                        inputError = uiState.inputError,
                        image1 = uiState.image1,
                        image2 = uiState.image2,
                        image3 = uiState.image3,
                        onQuantityChange = viewModel::onQuantityChange,
                        onStart = { viewModel.onStartSend { id, qty -> onStartSend(id, qty) } },
                        onStartBackground = {
                            val q = viewModel.getValidatedQuantity() ?: return@DetailsContent
                            val data = Data.Builder()
                                .putInt(SendingWorker.KEY_CAMPAIGN_ID, uiState.campaign!!.summary.id)
                                .putInt(SendingWorker.KEY_QUANTITY, q)
                                .build()
                            val req = OneTimeWorkRequestBuilder<SendingWorker>()
                                .setInputData(data)
                                .build()
                            WorkManager.getInstance(context).enqueue(req)
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsContent(
    full: CampaignFull,
    quantityText: String,
    maxQuantity: Int,
    inputError: String?,
    onQuantityChange: (String) -> Unit,
    onStart: () -> Unit,
    onStartBackground: () -> Unit,
    image1: ByteArray?,
    image2: ByteArray?,
    image3: ByteArray?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InfoCard(full) }

        // Mensajes 1,2,3 en cards separadas con imagen o placeholder
        item { MessageCard(index = 1, text = full.summary.mensaje1 ?: "", imageBytes = image1) }
        item { MessageCard(index = 2, text = full.summary.mensaje2 ?: "", imageBytes = image2) }
        item { MessageCard(index = 3, text = full.summary.mensaje3 ?: "", imageBytes = image3) }

        item {
            Card { Column(Modifier.padding(16.dp)) {
                Text("Contactos", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(full.contacts.size) { idx ->
                            val c = full.contacts[idx]
                            Text("${c.secuencia}. ${c.nombre} - ${c.telefono} (${c.estado ?: "PEN"})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }}
        }

        item {
            Card { Column(Modifier.padding(16.dp)) {
                Text("Configuraci??n de env??o", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("M??ximo permitido: ${maxQuantity} (tope app: 50)")
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = onQuantityChange,
                        label = { Text("Cantidad a enviar") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = inputError != null,
                        supportingText = { inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                            Text("Enviar")
                        }
                        OutlinedButton(onClick = onStartBackground, modifier = Modifier.weight(1f)) {
                            Text("Segundo plano")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Intervalo entre mensajes: 20-50s (fijo por ahora)", style = MaterialTheme.typography.bodySmall)
            }}
        }
    }
}

@Composable
private fun InfoCard(full: CampaignFull) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(full.summary.descripcion, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Oficina: ${full.summary.oficina}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mensajes: ${full.messages.size}")
                Text("Contactos: ${full.totalContacts}")
            }
        }
    }
}

@Composable
private fun MessageCard(index: Int, text: String, imageBytes: ByteArray?) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Mensaje $index", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (imageBytes != null) {
                val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Imagen del mensaje $index",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sin imagen")
                }
                Spacer(Modifier.height(8.dp))
            }
            if (text.isNotBlank()) {
                Text(text)
            } else {
                Text("(Sin texto)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


