package com.whatsappbulk.sender.ui.screens.campaigns

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.whatsappbulk.sender.domain.model.CampaignFull
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
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh al volver a esta pantalla
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles de campaña") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                        segundosDesde = uiState.segundosDesde,
                        segundosHasta = uiState.segundosHasta,
                        cantidadMaxDia = uiState.cantidadMaxDia,
                        mensajesEnviadosHoy = uiState.mensajesEnviadosHoy,
                        cuotaDisponible = uiState.cuotaDisponible,
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
    segundosDesde: Int,
    segundosHasta: Int,
    cantidadMaxDia: Int,
    mensajesEnviadosHoy: Int,
    cuotaDisponible: Int,
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
                Text("Contactos (${full.contacts.size})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                // Tabla con scroll horizontal y vertical
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    val horizontalScrollState = rememberScrollState()

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.horizontalScroll(horizontalScrollState)
                    ) {
                        // Cabecera de la tabla
                        item {
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(8.dp)
                            ) {
                                TableHeaderCell("Sec", width = 50.dp)
                                TableHeaderCell("Socio", width = 80.dp)
                                TableHeaderCell("Nombre", width = 200.dp)
                                TableHeaderCell("Teléfono", width = 120.dp)
                                TableHeaderCell("Estado", width = 70.dp)
                                TableHeaderCell("Intentos", width = 70.dp)
                                TableHeaderCell("Fecha/Hora", width = 150.dp)
                                TableHeaderCell("Mensaje Indiv.", width = 250.dp)
                            }
                        }

                        // Filas de datos
                        items(full.contacts.size) { idx ->
                            val c = full.contacts[idx]
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (idx % 2 == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(8.dp)
                            ) {
                                TableCell(c.secuencia.toString(), width = 50.dp)
                                TableCell(c.numeroSocio.toString(), width = 80.dp)
                                TableCell(c.nombre, width = 200.dp)
                                TableCell(c.telefono, width = 120.dp)
                                TableCell(c.estado ?: "PEN", width = 70.dp)
                                TableCell(c.contadorIntentos.toString(), width = 70.dp)
                                TableCell(c.fechaHoraEstado ?: "-", width = 150.dp)
                                TableCell(c.mensajeIndividual ?: "-", width = 250.dp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Desliza horizontalmente para ver todos los campos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }}
        }

        item {
            Card { Column(Modifier.padding(16.dp)) {
                Text("Configuración de envío", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Enviados hoy: ${mensajesEnviadosHoy} / ${cantidadMaxDia}")
                Text("Cuota disponible: ${cuotaDisponible} mensajes")
                Text("Máximo por envío: ${maxQuantity}")
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
                Text("Intervalo entre mensajes: ${segundosDesde}-${segundosHasta}s", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun TableHeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TableCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
