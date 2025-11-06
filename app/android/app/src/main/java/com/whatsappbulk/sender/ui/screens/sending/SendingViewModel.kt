package com.whatsappbulk.sender.ui.screens.sending

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.model.CampaignContact
import com.whatsappbulk.sender.domain.model.CampaignFull
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.repository.ICampaignRepository
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
import kotlin.random.Random

data class SendingUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val campaignId: Int? = null,
    val campaignName: String = "",
    val total: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
    val currentContact: String? = null,
    val currentMessage: String? = null,
    val nextInSeconds: Int = 0,
    val isPaused: Boolean = false,
    val isCancelled: Boolean = false,
    val isCompleted: Boolean = false
)

@HiltViewModel
class SendingViewModel @Inject constructor(
    private val whatsappRepository: IWhatsAppRepository,
    private val campaignRepository: ICampaignRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SendingUiState())
    val uiState: StateFlow<SendingUiState> = _uiState.asStateFlow()

    private val campaignId: Int = savedStateHandle.get<Int>("campaignId") ?: -1
    private val quantityArg: Int = savedStateHandle.get<Int>("quantity") ?: 0

    private var sendJob: Job? = null
    private var countdownJob: Job? = null

    private var campaignFull: CampaignFull? = null
    private var targets: List<CampaignContact> = emptyList()
    private val imageCache = mutableMapOf<Int, ByteArray?>()

    init {
        start()
    }

    fun start() {
        if (campaignId <= 0) {
            _uiState.update { it.copy(isLoading = false, error = "ID de campana invalido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = campaignRepository.getCampaignFull(campaignId)) {
                is Result.Success -> {
                    campaignFull = result.data
                    val full = result.data
                    val pendingContacts = full.contacts.filter { it.estado == null || it.estado == "PEN" }
                    val available = pendingContacts.size
                    val messages = full.messages
                    val maxAllowed = minOf(50, available)
                    val toSend = quantityArg.coerceIn(1, maxAllowed.takeIf { it > 0 } ?: 1)
                    targets = pendingContacts.take(toSend)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            campaignId = campaignId,
                            campaignName = full.summary.descripcion,
                            total = toSend,
                            pending = toSend,
                            sent = 0,
                            failed = 0,
                            isPaused = false,
                            isCancelled = false,
                            isCompleted = false,
                            nextInSeconds = 0
                        )
                    }

                    launchSendLoop(messages)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    private fun launchSendLoop(messages: List<String>) {
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            for ((index, contact) in targets.withIndex()) {
                // Manejo de pausa/cancelacion
                while (uiState.value.isPaused && !uiState.value.isCancelled) {
                    delay(200)
                }
                if (uiState.value.isCancelled) break

                // Preparar mensaje (rotacion 1,2,3,...)
                val msgIndex = if (messages.isNotEmpty()) index % messages.size else 0
                val msg = buildMessageForContact(contact, messages, index)
                _uiState.update {
                    it.copy(
                        currentContact = "${contact.nombre} (${contact.telefono})",
                        currentMessage = msg
                    )
                }

                // Determinar si este mensaje lleva imagen
                val sendResult = if (shouldSendImage(msgIndex)) {
                    val imgNumber = msgIndex + 1 // 1..3
                    val imgBytes = getImageBytes(imgNumber)
                    if (imgBytes != null) {
                        whatsappRepository.sendImage(
                            phone = normalizePhone(contact.telefono),
                            imageBytes = imgBytes,
                            caption = msg
                        )
                    } else {
                        // Fallback a texto si no hay imagen disponible
                        whatsappRepository.sendTestMessage(
                            phone = normalizePhone(contact.telefono),
                            message = msg
                        )
                    }
                } else {
                    whatsappRepository.sendTestMessage(
                        phone = normalizePhone(contact.telefono),
                        message = msg
                    )
                }

                if (sendResult is Result.Success) {
                    // Actualizar estado en backend campaÃ±as
                    campaignRepository.updateMessageStatus(campaignId, contact.secuencia, "ENV")
                    _uiState.update {
                        val newSent = it.sent + 1
                        it.copy(
                            sent = newSent,
                            pending = it.total - newSent - it.failed
                        )
                    }
                } else {
                    campaignRepository.updateMessageStatus(campaignId, contact.secuencia, "FAL")
                    _uiState.update {
                        val newFailed = it.failed + 1
                        it.copy(
                            failed = newFailed,
                            pending = it.total - it.sent - newFailed
                        )
                    }
                }

                // Si no es el Ãºltimo, esperar intervalo 20-50s
                val isLast = index == targets.lastIndex
                if (!isLast && !uiState.value.isCancelled) {
                    val waitSec = Random.nextInt(20, 51)
                    startCountdown(waitSec)
                    var remaining = waitSec
                    while (remaining > 0 && !uiState.value.isCancelled) {
                        // Respetar pausa
                        while (uiState.value.isPaused && !uiState.value.isCancelled) {
                            delay(200)
                        }
                        delay(1000)
                        remaining -= 1
                        _uiState.update { it.copy(nextInSeconds = remaining) }
                    }
                    stopCountdown()
                }
            }

            _uiState.update {
                it.copy(
                    isCompleted = !it.isCancelled,
                    currentContact = null,
                    currentMessage = null,
                    nextInSeconds = 0
                )
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        _uiState.update { it.copy(nextInSeconds = seconds) }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(nextInSeconds = 0) }
    }

    fun pause() { _uiState.update { it.copy(isPaused = true) } }
    fun resume() { _uiState.update { it.copy(isPaused = false) } }
    fun cancel() { _uiState.update { it.copy(isCancelled = true) } }

    // Background envÃ­o se inicia desde la UI usando WorkManager

    private fun buildMessageForContact(contact: CampaignContact, messages: List<String>, index: Int): String {
        if (messages.isEmpty()) return contact.nombre
        val msg = messages[index % messages.size]
        // ConcatenaciÃ³n: nombre + mensajeX
        return "${contact.nombre} ${msg}"
    }

    private fun shouldSendImage(msgIndex: Int): Boolean {
        val full = campaignFull ?: return false
        return when (msgIndex) {
            0 -> full.summary.tieneImagen1
            1 -> full.summary.tieneImagen2
            2 -> full.summary.tieneImagen3
            else -> false
        }
    }

    private suspend fun getImageBytes(imageNumber: Int): ByteArray? {
        // 1) Intentar desde caché en memoria poblada por CampaignDetails
        com.whatsappbulk.sender.util.ImageMemoryCache.get(campaignId, imageNumber)?.let {
            imageCache[imageNumber] = it
            return it
        }
        // 2) Intentar caché local de esta sesión
        imageCache[imageNumber]?.let { return it }
        // 3) Descargar solo si no existe
        val result = campaignRepository.getCampaignImage(campaignId, imageNumber)
        return if (result is Result.Success) {
            imageCache[imageNumber] = result.data
            result.data
        } else null
    }

    private fun normalizePhone(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("@")) return trimmed
        val digits = trimmed.filter { it.isDigit() }
        return digits
    }
}

