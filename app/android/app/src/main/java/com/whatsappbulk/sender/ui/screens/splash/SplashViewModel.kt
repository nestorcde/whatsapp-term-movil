package com.whatsappbulk.sender.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.model.ConnectionStatus
import com.whatsappbulk.sender.domain.usecase.CheckConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val isChecking: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Checking,
    val errorMessage: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkConnectivity: CheckConnectivityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    fun runHealthCheck(onResult: (isConnected: Boolean, fortiInstalled: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, connectionStatus = ConnectionStatus.Checking, errorMessage = null) }
            val status = checkConnectivity()
            when (status.connectionStatus) {
                is ConnectionStatus.Connected -> {
                    _uiState.update { it.copy(isChecking = false, connectionStatus = ConnectionStatus.Connected) }
                    onResult(true, status.isFortiClientInstalled)
                }
                is ConnectionStatus.Error, ConnectionStatus.Disconnected -> {
                    _uiState.update { it.copy(isChecking = false, connectionStatus = ConnectionStatus.Disconnected) }
                    onResult(false, status.isFortiClientInstalled)
                }
                ConnectionStatus.Checking -> {
                    _uiState.update { it.copy(isChecking = true, connectionStatus = ConnectionStatus.Checking) }
                }
            }
        }
    }
}

