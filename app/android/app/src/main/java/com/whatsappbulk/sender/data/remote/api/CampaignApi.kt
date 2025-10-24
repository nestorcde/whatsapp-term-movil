package com.whatsappbulk.sender.data.remote.api

import com.whatsappbulk.sender.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API para el backend de campañas (Oracle DB - Puerto 3001)
 */
interface CampaignApi {

    /**
     * Autenticación de usuario
     */
    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Verificar token actual
     */
    @GET("/api/auth/verify")
    suspend fun verifyToken(): Response<ApiResponse<UserDto>>

    /**
     * Listar campañas disponibles para el usuario autenticado
     */
    @GET("/api/campaigns")
    suspend fun getCampaigns(): Response<ApiResponse<List<CampaignDto>>>

    /**
     * Obtener detalles de una campaña específica
     * @param campaignId ID de la campaña
     * @param limit Límite de contactos a devolver (opcional)
     */
    @GET("/api/campaigns/{id}/details")
    suspend fun getCampaignDetails(
        @Path("id") campaignId: Int,
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<List<CampaignDetailDto>>>

    /**
     * Obtener imagen de una campaña
     * @param campaignId ID de la campaña
     * @param imageNumber Número de imagen (1, 2 o 3)
     */
    @GET("/api/campaigns/{id}/image/{imageNumber}")
    suspend fun getCampaignImage(
        @Path("id") campaignId: Int,
        @Path("imageNumber") imageNumber: Int
    ): Response<ResponseBody>

    /**
     * Actualizar estado de un mensaje
     */
    @POST("/api/campaigns/update-status")
    suspend fun updateMessageStatus(
        @Body request: UpdateMessageStatusRequest
    ): Response<ApiResponse<Unit>>
}
