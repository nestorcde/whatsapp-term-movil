package com.whatsappbulk.sender.domain.repository

import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.model.WhatsAppSession

interface IWhatsAppRepository {

    /**
     * Iniciar sesión con número de teléfono (obtendrá código de 8 dígitos)
     */
    suspend fun startSessionWithPhone(phoneNumber: String): Result<WhatsAppSession>

    /**
     * Iniciar sesión con QR
     */
    suspend fun startSessionWithQR(): Result<WhatsAppSession>

    /**
     * Obtener el estado actual de la sesión
     */
    suspend fun getSessionStatus(): Result<WhatsAppSession>

    /**
     * Obtener el código de vinculación de 8 dígitos
     */
    suspend fun getLinkCode(): Result<String>

    /**
     * Verificar si hay una sesión activa
     */
    suspend fun checkConnection(): Result<Boolean>

    /**
     * Cerrar la sesión actual
     */
    suspend fun closeSession(): Result<Unit>

    /**
     * Enviar mensaje de prueba
     */
    suspend fun sendTestMessage(phone: String, message: String): Result<String>

    /**
     * Enviar mensaje de texto
     */
    suspend fun sendMessage(phone: String, message: String): Result<String>

    /**
     * Enviar imagen con mensaje
     */
    suspend fun sendImage(phone: String, imageBytes: ByteArray, caption: String): Result<String>
}
