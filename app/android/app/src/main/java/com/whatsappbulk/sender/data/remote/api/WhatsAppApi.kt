package com.whatsappbulk.sender.data.remote.api

import com.whatsappbulk.sender.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface WhatsAppApi {

    // ========== AUTH ENDPOINTS ==========

    /**
     * Iniciar sesión de WhatsApp
     * - Sin body: Inicia con QR
     * - Con phoneNumber: Inicia con código de vinculación
     */
    @POST("api/auth/start")
    suspend fun startSession(
        @Body request: StartSessionRequest = StartSessionRequest()
    ): Response<ApiResponse<SessionStatusDto>>

    /**
     * Obtener estado de la sesión
     */
    @GET("api/auth/status")
    suspend fun getStatus(): Response<ApiResponse<SessionStatusDto>>

    /**
     * Obtener código QR (solo si se inició con QR)
     */
    @GET("api/auth/qr")
    suspend fun getQRCode(): Response<ApiResponse<SessionStatusDto>>

    /**
     * Obtener código de vinculación de 8 dígitos (solo si se inició con phoneNumber)
     */
    @GET("api/auth/link-code")
    suspend fun getLinkCode(): Response<ApiResponse<LinkCodeDto>>

    /**
     * Verificar si está conectado
     */
    @GET("api/auth/check")
    suspend fun checkConnection(): Response<ApiResponse<Map<String, Boolean>>>

    /**
     * Cerrar sesión
     */
    @POST("api/auth/close")
    suspend fun closeSession(): Response<ApiResponse<Unit>>

    // ========== MESSAGE ENDPOINTS ==========

    /**
     * Enviar mensaje de texto
     */
    @POST("api/messages/send")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<ApiResponse<MessageDto>>

    /**
     * Obtener mensajes recibidos
     */
    @GET("api/messages")
    suspend fun getMessages(): Response<ApiResponse<List<Any>>>

    /**
     * Limpiar cola de mensajes
     */
    @DELETE("api/messages")
    suspend fun clearMessages(): Response<ApiResponse<Unit>>
}
