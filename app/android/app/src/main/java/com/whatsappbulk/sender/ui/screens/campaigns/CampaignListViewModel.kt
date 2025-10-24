package com.whatsappbulk.sender.ui.screens.campaigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.model.Campaign
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.repository.ICampaignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CampaignListUiState(
    val isLoading: Boolean = false,
    val campaigns: List<Campaign> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CampaignListViewModel @Inject constructor(
    private val campaignRepository: ICampaignRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampaignListUiState())
    val uiState: StateFlow<CampaignListUiState> = _uiState.asStateFlow()

    init {
        loadCampaigns()
    }

    fun loadCampaigns() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = campaignRepository.getCampaigns()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            campaigns = result.data,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {
                    // Ya está en loading state
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
