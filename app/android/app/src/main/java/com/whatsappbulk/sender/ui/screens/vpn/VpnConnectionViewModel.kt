package com.whatsappbulk.sender.ui.screens.vpn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.model.ConnectionStatus
import com.whatsappbulk.sender.domain.model.VpnStatus
import com.whatsappbulk.sender.domain.repository.IVpnRepository
import com.whatsappbulk.sender.domain.usecase.CheckConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VpnConnectionUiState(
    val isChecking: Boolean = false,
    val isFortiClientInstalled: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val lastCheckTime: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class VpnConnectionViewModel @Inject constructor(
    private val checkConnectivity: CheckConnectivityUseCase,
    private val vpnRepository: IVpnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VpnConnectionUiState())
    val uiState: StateFlow<VpnConnectionUiState> = _uiState.asStateFlow()

    fun refreshFortiInstalled() {
        _uiState.update { it.copy(isFortiClientInstalled = vpnRepository.isFortiClientInstalled()) }
    }

    fun openFortiClient() {
        if (!_uiState.value.isFortiClientInstalled) {
            _uiState.update { it.copy(errorMessage = "FortiClient no está instalado") }
            return
        }
        val result = vpnRepository.launchFortiClient()
        if (result is com.whatsappbulk.sender.domain.model.Result.Error) {
            _uiState.update { it.copy(errorMessage = result.message) }
        }
    }

    fun openPlayStore(): Boolean {
        val result = vpnRepository.openPlayStoreFortiClient()
        if (result is com.whatsappbulk.sender.domain.model.Result.Error) {
            _uiState.update { it.copy(errorMessage = result.message) }
            return false
        }
        return true
    }

    fun verifyConnection(onConnected: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            val status: VpnStatus = checkConnectivity()
            val now = System.currentTimeMillis()
            when (status.connectionStatus) {
                is ConnectionStatus.Connected -> {
                    _uiState.update { it.copy(isChecking = false, connectionStatus = ConnectionStatus.Connected, lastCheckTime = now) }
                    onConnected()
                }
                is ConnectionStatus.Error, ConnectionStatus.Disconnected -> {
                    _uiState.update { it.copy(isChecking = false, connectionStatus = ConnectionStatus.Disconnected, lastCheckTime = now, isFortiClientInstalled = status.isFortiClientInstalled, errorMessage = (status.connectionStatus as? ConnectionStatus.Error)?.message) }
                }
                ConnectionStatus.Checking -> {
                    _uiState.update { it.copy(isChecking = true) }
                }
            }
        }
    }
}

