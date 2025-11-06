package com.whatsappbulk.sender.ui.screens.whatsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.model.SessionStatus
import com.whatsappbulk.sender.domain.repository.IWhatsAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsAppViewModel @Inject constructor(
    private val whatsAppRepository: IWhatsAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsAppUiState())
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Verificar si ya hay una sesión activa al abrir la pantalla
        checkExistingSession()
    }

    /**
     * Verificar si ya existe una sesión activa
     */
    fun checkExistingSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }

            when (val result = whatsAppRepository.checkConnection()) {
                is Result.Success -> {
                    if (result.data) {
                        // Ya hay sesión activa
                        _uiState.update {
                            it.copy(
                                isChecking = false,
                                sessionStatus = SessionStatus.CONNECTED,
                                statusMessage = "Sesión activa encontrada ✅"
                            )
                        }
                    } else {
                        // No hay sesión
                        _uiState.update {
                            it.copy(
                                isChecking = false,
                                sessionStatus = SessionStatus.DISCONNECTED,
                                statusMessage = "No hay sesión activa. Inicia sesión con tu número."
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            sessionStatus = SessionStatus.DISCONNECTED,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Cambio en el campo de número de teléfono
     */
    fun onPhoneNumberChange(phoneNumber: String) {
        _uiState.update {
            it.copy(
                phoneNumber = phoneNumber,
                errorMessage = null
            )
        }
    }

    /**
     * Iniciar sesión con número de teléfono
     */
    fun startSessionWithPhone() {
        val phoneNumber = _uiState.value.phoneNumber.trim()

        // Validar número
        if (phoneNumber.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Ingresa un número de teléfono")
            }
            return
        }

        if (phoneNumber.length < 10) {
            _uiState.update {
                it.copy(errorMessage = "Número de teléfono inválido. Formato: {códigoPaís}{número}")
            }
            return
        }

        viewModelScope.launch {
            // Primero cerrar cualquier sesión anterior
            pollingJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    linkCode = null, // Limpiar código anterior
                    statusMessage = "Cerrando sesión anterior..."
                )
            }

            // Cerrar sesión anterior si existe
            whatsAppRepository.closeSession()

            // Esperar un poco para asegurar que se cerró
            delay(1000)

            _uiState.update {
                it.copy(statusMessage = "Iniciando nueva sesión...")
            }

            when (val result = whatsAppRepository.startSessionWithPhone(phoneNumber)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = true, // Seguir cargando mientras se obtiene el código
                            sessionStatus = result.data.status,
                            statusMessage = "Generando código de vinculación...",
                            errorMessage = null
                        )
                    }

                    // Iniciar polling del estado de inmediato
                    startPollingStatus()
                    // Y en paralelo intentar obtener el código de vinculación
                    getLinkCode()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessionStatus = SessionStatus.DISCONNECTED,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Obtener el código de vinculación de 8 dígitos
     */
    private fun getLinkCode() {
        viewModelScope.launch {
            when (val result = whatsAppRepository.getLinkCode()) {
                is Result.Success -> {
                    val code = result.data
                    val formattedCode = code.chunked(4).joinToString("-") // 1234-5678

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            linkCode = formattedCode,
                            sessionStatus = SessionStatus.LINK_CODE_READY,
                            statusMessage = "Código generado. Verifica en WhatsApp.",
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    // Gracia: esperar unos segundos antes de mostrar error,
                    // e intentar obtener el código desde el estado.
                    val start = System.currentTimeMillis()
                    val graceMs = 15000L
                    var resolved = false
                    while (System.currentTimeMillis() - start < graceMs) {
                        delay(2000)
                        when (val st = whatsAppRepository.getSessionStatus()) {
                            is Result.Success -> {
                                val session = st.data
                                val newLinkCode = session.linkCode?.chunked(4)?.joinToString("-")
                                if (!newLinkCode.isNullOrEmpty() || session.status == SessionStatus.CONNECTED) {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            linkCode = newLinkCode ?: it.linkCode,
                                            sessionStatus = session.status,
                                            statusMessage = if (session.status == SessionStatus.CONNECTED)
                                                "¡Conectado exitosamente! ✅" else "Código generado. Verifica en WhatsApp.",
                                            errorMessage = null
                                        )
                                    }
                                    resolved = true
                                    break
                                }
                            }
                            else -> { /* ignorar errores transitorios */ }
                        }
                    }
                    if (!resolved) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "No se pudo obtener el código. ${result.message}"
                            )
                        }
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Iniciar polling del estado de la sesión
     */
    private fun startPollingStatus() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var previousStatus: SessionStatus? = null
            while (true) {
                delay(3000) // Polling cada 3 segundos

                when (val result = whatsAppRepository.getSessionStatus()) {
                    is Result.Success -> {
                        val session = result.data

                        // Si hay linkCode en la sesión y no tenemos uno, usarlo
                        val currentLinkCode = _uiState.value.linkCode
                        val newLinkCode = session.linkCode?.chunked(4)?.joinToString("-")

                        // Detectar pérdida de sesión: si antes estaba conectado y ahora no
                        if (previousStatus == SessionStatus.CONNECTED && session.status != SessionStatus.CONNECTED) {
                            _uiState.update {
                                it.copy(
                                    sessionStatus = session.status,
                                    statusMessage = "La sesión de WhatsApp se perdió.",
                                    sessionLost = true,
                                    errorMessage = null
                                )
                            }
                            // Detener el polling hasta que el usuario decida reiniciar
                            pollingJob?.cancel()
                            return@launch
                        }

                        _uiState.update {
                            it.copy(
                                sessionStatus = session.status,
                                linkCode = if (currentLinkCode == null && newLinkCode != null) {
                                    newLinkCode
                                } else {
                                    currentLinkCode
                                },
                                statusMessage = when (session.status) {
                                    SessionStatus.CONNECTED -> "¡Conectado exitosamente! ✅"
                                    SessionStatus.CONNECTING -> "Conectando..."
                                    SessionStatus.LINK_CODE_READY -> "Esperando confirmación en WhatsApp..."
                                    SessionStatus.FAILED -> "Error en la conexión"
                                    else -> it.statusMessage
                                },
                                errorMessage = null
                            )
                        }

                        // Detener polling si se conectó o falló
                        if (session.status == SessionStatus.CONNECTED ||
                            session.status == SessionStatus.FAILED) {
                            pollingJob?.cancel()
                        }

                        previousStatus = session.status
                    }
                    is Result.Error -> {
                        // No actualizar error durante polling para no molestar
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    /**
     * Usuario confirma que vio el aviso de sesión perdida y desea volver a inicio.
     * Cierra la sesión remota y resetea el estado a desconectado.
     */
    fun handleSessionLostReturnToLogin() {
        viewModelScope.launch {
            pollingJob?.cancel()
            whatsAppRepository.closeSession()
            _uiState.update {
                WhatsAppUiState(
                    sessionStatus = SessionStatus.DISCONNECTED,
                    statusMessage = "Sesión finalizada. Puedes iniciar una nueva.",
                    sessionLost = false
                )
            }
        }
    }

    /**
     * Enviar mensaje de prueba
     */
    fun sendTestMessage() {
        val testPhone = _uiState.value.testPhone.trim()

        if (testPhone.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Ingresa un número para enviar mensaje de prueba")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSendingTest = true,
                    testMessageStatus = null,
                    errorMessage = null
                )
            }

            when (val result = whatsAppRepository.sendTestMessage(
                testPhone,
                "Hola! Este es un mensaje de prueba desde WhatsApp Bulk Sender 📱"
            )) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSendingTest = false,
                            testMessageStatus = "Mensaje enviado exitosamente ✅",
                            testPhone = ""
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSendingTest = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onTestPhoneChange(phone: String) {
        _uiState.update {
            it.copy(
                testPhone = phone,
                testMessageStatus = null,
                errorMessage = null
            )
        }
    }

    /**
     * Reintentar obtener código de vinculación
     */
    fun retryGetLinkCode() {
        _uiState.update {
            it.copy(
                isLoading = true,
                linkCode = null,
                errorMessage = null,
                statusMessage = "Generando nuevo código..."
            )
        }
        getLinkCode()
    }

    /**
     * Cancelar y volver al estado desconectado
     */
    fun cancelAndGoBack() {
        viewModelScope.launch {
            // Cancelar polling
            pollingJob?.cancel()

            // Cerrar sesión en el servidor
            whatsAppRepository.closeSession()

            // Resetear state
            _uiState.update {
                WhatsAppUiState(
                    sessionStatus = SessionStatus.DISCONNECTED,
                    statusMessage = "Sesión cancelada. Puedes iniciar una nueva."
                )
            }
        }
    }

    /**
     * Cerrar sesión
     */
    fun closeSession() {
        viewModelScope.launch {
            pollingJob?.cancel()

            _uiState.update { it.copy(isLoading = true) }

            when (whatsAppRepository.closeSession()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessionStatus = SessionStatus.DISCONNECTED,
                            linkCode = null,
                            phoneNumber = "",
                            statusMessage = "Sesión cerrada",
                            testMessageStatus = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error al cerrar sesión"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

// UI State
data class WhatsAppUiState(
    val isChecking: Boolean = false,
    val isLoading: Boolean = false,
    val isSendingTest: Boolean = false,
    val phoneNumber: String = "",
    val linkCode: String? = null,
    val sessionStatus: SessionStatus = SessionStatus.DISCONNECTED,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val testPhone: String = "",
    val testMessageStatus: String? = null,
    val sessionLost: Boolean = false
)
