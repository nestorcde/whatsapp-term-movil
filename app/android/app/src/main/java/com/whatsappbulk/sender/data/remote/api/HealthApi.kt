package com.whatsappbulk.sender.data.remote.api

import com.whatsappbulk.sender.data.remote.dto.HealthResponse
import retrofit2.Response
import retrofit2.http.GET

interface HealthApi {
    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>
}

