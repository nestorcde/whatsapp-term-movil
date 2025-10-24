package com.whatsappbulk.sender.ui.screens.campaigns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.model.CampaignFull
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.repository.ICampaignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CampaignDetailsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val campaign: CampaignFull? = null,
    val quantityText: String = "",
    val maxQuantity: Int = 0,
    val inputError: String? = null,
    val image1: ByteArray? = null,
    val image2: ByteArray? = null,
    val image3: ByteArray? = null,
    val segundosDesde: Int = 20,
    val segundosHasta: Int = 40,
    val cantidadMaxDia: Int = 200,
    val mensajesEnviadosHoy: Int = 0,
    val cuotaDisponible: Int = 200
)

@HiltViewModel
class CampaignDetailsViewModel @Inject constructor(
    private val campaignRepository: ICampaignRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampaignDetailsUiState(isLoading = true))
    val uiState: StateFlow<CampaignDetailsUiState> = _uiState.asStateFlow()

    private val campaignId: Int = savedStateHandle.get<Int>("campaignId") ?: -1

    init {
        load()
    }

    fun load() {
        if (campaignId <= 0) {
            _uiState.update { it.copy(isLoading = false, error = "ID de campana invalido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Obtener mensajes enviados hoy
            val sentToday = when (val sentResult = campaignRepository.getMessagesSentToday()) {
                is Result.Success -> sentResult.data
                else -> 0
            }

            when (val result = campaignRepository.getCampaignFull(campaignId)) {
                is Result.Success -> {
                    val full = result.data
                    val pending = full.contacts.count { it.estado == null || it.estado == "PEN" }

                    // Usar configuración del backend o valores por defecto
                    val config = full.summary.config
                    val maxDiario = config?.cantidadMaxDia ?: 200
                    val cuotaDisp = maxOf(0, maxDiario - sentToday)
                    val max = minOf(50, pending, cuotaDisp)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            campaign = full,
                            maxQuantity = max,
                            quantityText = if (it.quantityText.isBlank()) "10" else it.quantityText,
                            inputError = null,
                            segundosDesde = config?.segundosDesde ?: 20,
                            segundosHasta = config?.segundosHasta ?: 40,
                            cantidadMaxDia = maxDiario,
                            mensajesEnviadosHoy = sentToday,
                            cuotaDisponible = cuotaDisp
                        )
                    }

                    // Cargar imagenes asociadas a mensajes si existen
                    loadImages(full.summary.id, full)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onQuantityChange(text: String) {
        // Permitir solo digitos y vacio para facilitar edicion
        val filtered = text.filter { it.isDigit() }
        _uiState.update { it.copy(quantityText = filtered, inputError = null) }
    }

    fun onStartSend(navigate: (campaignId: Int, quantity: Int) -> Unit) {
        val state = _uiState.value
        val q = state.quantityText.filter { it.isDigit() }.toIntOrNull() ?: 0
        when {
            q <= 0 -> _uiState.update { it.copy(inputError = "La cantidad debe ser mayor a 0") }
            q > state.maxQuantity -> _uiState.update { it.copy(inputError = "La cantidad supera los contactos disponibles (${state.maxQuantity})") }
            else -> navigate(campaignId, q)
        }
    }

    fun getValidatedQuantity(): Int? {
        val state = _uiState.value
        val q = state.quantityText.filter { it.isDigit() }.toIntOrNull() ?: 0
        return if (q <= 0) {
            _uiState.update { it.copy(inputError = "La cantidad debe ser mayor a 0") }
            null
        } else if (q > state.maxQuantity) {
            _uiState.update { it.copy(inputError = "La cantidad supera los contactos disponibles (${state.maxQuantity})") }
            null
        } else q
    }

    private fun loadImages(campaignId: Int, full: CampaignFull) {
        viewModelScope.launch {
            // Mensaje 1
            if (full.summary.tieneImagen1) {
                when (val r = campaignRepository.getCampaignImage(campaignId, 1)) {
                    is Result.Success -> _uiState.update { it.copy(image1 = r.data) }
                    else -> Unit
                }
            }
            // Mensaje 2
            if (full.summary.tieneImagen2) {
                when (val r = campaignRepository.getCampaignImage(campaignId, 2)) {
                    is Result.Success -> _uiState.update { it.copy(image2 = r.data) }
                    else -> Unit
                }
            }
            // Mensaje 3
            if (full.summary.tieneImagen3) {
                when (val r = campaignRepository.getCampaignImage(campaignId, 3)) {
                    is Result.Success -> _uiState.update { it.copy(image3 = r.data) }
                    else -> Unit
                }
            }
        }
    }
}


