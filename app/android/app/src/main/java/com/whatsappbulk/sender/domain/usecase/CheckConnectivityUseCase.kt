package com.whatsappbulk.sender.domain.usecase

import com.whatsappbulk.sender.domain.model.ConnectionStatus
import com.whatsappbulk.sender.domain.model.VpnStatus
import com.whatsappbulk.sender.domain.repository.IVpnRepository
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class CheckConnectivityUseCase @Inject constructor(
    private val vpnRepository: IVpnRepository
) {
    suspend operator fun invoke(): VpnStatus {
        return try {
            val result = withTimeout(10_000L) { vpnRepository.checkBackendHealth() }
            when (result) {
                is com.whatsappbulk.sender.domain.model.Result.Success ->
                    VpnStatus(
                        isFortiClientInstalled = vpnRepository.isFortiClientInstalled(),
                        connectionStatus = if (result.data) ConnectionStatus.Connected else ConnectionStatus.Disconnected
                    )
                is com.whatsappbulk.sender.domain.model.Result.Error ->
                    VpnStatus(
                        isFortiClientInstalled = vpnRepository.isFortiClientInstalled(),
                        connectionStatus = ConnectionStatus.Error(result.message)
                    )
                is com.whatsappbulk.sender.domain.model.Result.Loading ->
                    VpnStatus(
                        isFortiClientInstalled = vpnRepository.isFortiClientInstalled(),
                        connectionStatus = ConnectionStatus.Checking
                    )
            }
        } catch (e: Exception) {
            VpnStatus(
                isFortiClientInstalled = vpnRepository.isFortiClientInstalled(),
                connectionStatus = ConnectionStatus.Error(e.message ?: "Error de conexión")
            )
        }
    }
}

