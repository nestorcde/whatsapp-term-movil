package com.whatsappbulk.sender.ui.screens.vpn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.repository.IVpnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VpnInstallUiState(
    val isNavigating: Boolean = false
)

sealed class NavigationEvent {
    object ToVpnConnection : NavigationEvent()
}

@HiltViewModel
class VpnInstallViewModel @Inject constructor(
    private val vpnRepository: IVpnRepository
) : ViewModel() {

    private val navigationChannel = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = navigationChannel.receiveAsFlow()

    fun onInstallClick() {
        viewModelScope.launch {
            vpnRepository.openPlayStoreFortiClient()
            navigationChannel.send(NavigationEvent.ToVpnConnection)
        }
    }
}

