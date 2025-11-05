package com.whatsappbulk.sender.domain.model

sealed class ConnectionStatus {
    object Connected : ConnectionStatus()
    object Disconnected : ConnectionStatus()
    object Checking : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

data class VpnStatus(
    val isFortiClientInstalled: Boolean,
    val connectionStatus: ConnectionStatus
)

