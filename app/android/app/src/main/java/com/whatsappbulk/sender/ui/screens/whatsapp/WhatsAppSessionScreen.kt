package com.whatsappbulk.sender.ui.screens.whatsapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsappbulk.sender.domain.model.SessionStatus
import com.whatsappbulk.sender.ui.theme.WhatsAppBulkSenderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSessionScreen(
    viewModel: WhatsAppViewModel = hiltViewModel(),
    onOpenCampaigns: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = com.whatsappbulk.sender.R.string.session_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isChecking) {
                // Estado de verificación inicial
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(32.dp)
                    )
                    Text(
                        text = ""+com.whatsappbulk.sender.R.string.verifying_session+"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (uiState.sessionLost) {
                // Pantalla de sesión perdida
                LostSessionContent(
                    onReturnToLogin = viewModel::handleSessionLostReturnToLogin
                )
            } else {
                // Contenido principal
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Indicador de estado
                    StatusCard(
                        status = uiState.sessionStatus,
                        message = uiState.statusMessage
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Contenido según el estado de la sesión
                    when (uiState.sessionStatus) {
                        SessionStatus.DISCONNECTED -> {
                            DisconnectedContent(
                                phoneNumber = uiState.phoneNumber,
                                onPhoneNumberChange = viewModel::onPhoneNumberChange,
                                onStartSession = viewModel::startSessionWithPhone,
                                isLoading = uiState.isLoading
                            )
                        }
                        SessionStatus.CONNECTING,
                        SessionStatus.LINK_CODE_READY -> {
                            LinkCodeContent(
                                linkCode = uiState.linkCode,
                                isLoading = uiState.isLoading,
                                onRetry = viewModel::retryGetLinkCode,
                                onCancel = viewModel::cancelAndGoBack
                            )
                        }
                        SessionStatus.CONNECTED -> {
                            ConnectedContent(
                                testPhone = uiState.testPhone,
                                onTestPhoneChange = viewModel::onTestPhoneChange,
                                onSendTest = viewModel::sendTestMessage,
                                isSending = uiState.isSendingTest,
                                testStatus = uiState.testMessageStatus,
                                onCloseSession = viewModel::closeSession,
                                onOpenCampaigns = onOpenCampaigns
                            )
                        }
                        else -> {}
                    }

                    // Mensaje de error
                    uiState.errorMessage?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    status: SessionStatus,
    message: String?
) {
    val (backgroundColor, textColor) = when (status) {
        SessionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        SessionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        SessionStatus.CONNECTING,
        SessionStatus.LINK_CODE_READY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == SessionStatus.CONNECTED) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Conectado",
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = when (status) {
                        SessionStatus.DISCONNECTED -> "Desconectado"
                        SessionStatus.CONNECTING -> "Conectando..."
                        SessionStatus.LINK_CODE_READY -> "Código de vinculación generado"
                        SessionStatus.CONNECTED -> "Conectado"
                        else -> "Estado desconocido"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun DisconnectedContent(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onStartSession: () -> Unit,
    isLoading: Boolean
) {
    Text(
        text = "Conectar WhatsApp",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Instrucciones:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "1. Ingresa tu número con código de país\n" +
                       "2. Recibirás un código de 8 dígitos en WhatsApp\n" +
                       "3. Verifica que coincida y confirma\n" +
                       "4. ¡Listo para enviar mensajes!",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = phoneNumber,
        onValueChange = onPhoneNumberChange,
        label = { Text("Número de teléfono") },
        placeholder = { Text("595973123456") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Teléfono"
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone
        ),
        supportingText = {
            Text("Formato: {códigoPaís}{número} sin + ni espacios")
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onStartSession,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading && phoneNumber.isNotBlank()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Iniciar Sesión",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun LinkCodeContent(
    linkCode: String?,
    isLoading: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading || linkCode == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "Generando código de vinculación...",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = "Código de Vinculación",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mostrar código grande
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = linkCode,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Verifica en WhatsApp:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "1. Abre WhatsApp en tu teléfono\n" +
                               "2. Ve a Dispositivos Vinculados\n" +
                               "3. Verifica que el código coincida\n" +
                               "4. Confirma la vinculación",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text(stringResource(id = com.whatsappbulk.sender.R.string.new_code))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = com.whatsappbulk.sender.R.string.code_expired_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ConnectedContent(
    testPhone: String,
    onTestPhoneChange: (String) -> Unit,
    onSendTest: () -> Unit,
    isSending: Boolean,
    testStatus: String?,
    onCloseSession: () -> Unit,
    onOpenCampaigns: () -> Unit
) {
    Text(
        text = "WhatsApp Conectado",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Mensaje de Prueba",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = testPhone,
                onValueChange = onTestPhoneChange,
                label = { Text(stringResource(id = com.whatsappbulk.sender.R.string.phone_label)) },
                placeholder = { Text(stringResource(id = com.whatsappbulk.sender.R.string.phone_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Teléfono"
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSendTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isSending && testPhone.isNotBlank()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = com.whatsappbulk.sender.R.string.send_test_message))
                }
            }

            testStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
        onClick = onCloseSession,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(id = com.whatsappbulk.sender.R.string.close_session))
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onOpenCampaigns,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(id = com.whatsappbulk.sender.R.string.go_to_campaigns))
    }
}

@Composable
fun LostSessionContent(
    onReturnToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sesión de WhatsApp perdida",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "La sesión se ha desconectado. Para evitar bloqueos por intentos de código, no generaremos uno automáticamente.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onReturnToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio de sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WhatsAppSessionScreenPreview() {
    WhatsAppBulkSenderTheme {
        WhatsAppSessionScreen(onOpenCampaigns = {})
    }
}








