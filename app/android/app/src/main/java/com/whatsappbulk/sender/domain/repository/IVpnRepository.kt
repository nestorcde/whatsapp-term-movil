package com.whatsappbulk.sender.domain.repository

import com.whatsappbulk.sender.domain.model.Result

interface IVpnRepository {
    suspend fun checkBackendHealth(): Result<Boolean>
    fun isFortiClientInstalled(): Boolean
    fun launchFortiClient(): Result<Unit>
    fun openPlayStoreFortiClient(): Result<Unit>
}

