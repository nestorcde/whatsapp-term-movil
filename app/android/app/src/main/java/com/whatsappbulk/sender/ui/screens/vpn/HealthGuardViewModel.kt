package com.whatsappbulk.sender.ui.screens.vpn

import androidx.lifecycle.ViewModel
import com.whatsappbulk.sender.domain.model.ConnectionStatus
import com.whatsappbulk.sender.domain.usecase.CheckConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HealthGuardViewModel @Inject constructor(
    private val checkConnectivity: CheckConnectivityUseCase
) : ViewModel() {
    suspend fun isConnected(): Boolean {
        val status = checkConnectivity()
        return status.connectionStatus is ConnectionStatus.Connected
    }
}
