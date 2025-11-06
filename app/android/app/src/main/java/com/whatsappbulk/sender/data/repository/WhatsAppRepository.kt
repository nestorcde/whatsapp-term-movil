package com.whatsappbulk.sender.data.repository

import com.whatsappbulk.sender.data.remote.api.WhatsAppApi
import com.whatsappbulk.sender.data.remote.dto.SendMessageRequest
import com.whatsappbulk.sender.data.remote.dto.StartSessionRequest
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.model.SessionStatus
import com.whatsappbulk.sender.domain.model.WhatsAppSession
import com.whatsappbulk.sender.domain.repository.IWhatsAppRepository
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppRepository @Inject constructor(
    private val api: WhatsAppApi
) : IWhatsAppRepository {

    override suspend fun startSessionWithPhone(phoneNumber: String): Result<WhatsAppSession> {
        return try {
            val response = api.startSession(StartSessionRequest(phoneNumber = phoneNumber))

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                Result.Success(
                    WhatsAppSession(
                        phoneNumber = phoneNumber,
                        linkCode = null,
                        status = mapStatus(data.status),
                        connectedAt = null
                    )
                )
            } else {
                Result.Error(
                    response.body()?.error ?: "Error al iniciar sesión"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                "Error de conexión: ${e.message ?: "Verifica que el servidor esté corriendo"}",
                e
            )
        }
    }

    override suspend fun startSessionWithQR(): Result<WhatsAppSession> {
        return try {
            val response = api.startSession(StartSessionRequest())

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                Result.Success(
                    WhatsAppSession(
                        phoneNumber = null,
                        linkCode = null,
                        status = mapStatus(data.status),
                        connectedAt = null
                    )
                )
            } else {
                Result.Error(
                    response.body()?.error ?: "Error al iniciar sesión con QR"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                "Error de conexión: ${e.message ?: "Verifica que el servidor esté corriendo"}",
                e
            )
        }
    }

    override suspend fun getSessionStatus(): Result<WhatsAppSession> {
        return try {
            val response = api.getStatus()

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                Result.Success(
                    WhatsAppSession(
                        phoneNumber = null,
                        linkCode = data.linkCode,
                        status = mapStatus(data.status),
                        connectedAt = if (data.isConnected) System.currentTimeMillis() else null
                    )
                )
            } else {
                Result.Error(
                    response.body()?.error ?: "Error al obtener estado"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                "Error de conexión: ${e.message}",
                e
            )
        }
    }

    override suspend fun getLinkCode(): Result<String> {
        return try {
            // Primero intentar obtener desde status (más rápido)
            delay(2000) // Esperar que el servidor genere el código

            val statusResponse = api.getStatus()

            if (statusResponse.isSuccessful && statusResponse.body()?.success == true) {
                val data = statusResponse.body()?.data
                val linkCodeFromStatus = data?.linkCode

                if (!linkCodeFromStatus.isNullOrEmpty()) {
                    // Ya tenemos el código desde status
                    return Result.Success(linkCodeFromStatus)
                }
            }

            // Si no está en status, intentar el endpoint específico
            delay(1000)
            val response = api.getLinkCode()

            if (response.isSuccessful && response.body()?.success == true) {
                val linkCode = response.body()?.data?.linkCode
                if (!linkCode.isNullOrEmpty()) {
                    Result.Success(linkCode)
                } else {
                    Result.Error("El código de vinculación está vacío")
                }
            } else {
                Result.Error(
                    response.body()?.error ?: "Error al obtener código de vinculación"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                "Error al obtener código: ${e.message ?: "Desconocido"}",
                e
            )
        }
    }

    override suspend fun checkConnection(): Result<Boolean> {
        return try {
            val response = api.checkConnection()
            if (response.isSuccessful && response.body()?.success == true) {
                val isConnected = response.body()?.data?.isConnected ?: false
                Result.Success(isConnected)
            } else {
                Result.Success(false)
            }
        } catch (e: Exception) {
            Result.Error("Error al verificar conexión: ${e.message}", e)
        }
    }

    override suspend fun closeSession(): Result<Unit> {
        return try {
            val response = api.closeSession()

            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(Unit)
            } else {
                Result.Error(
                    response.body()?.error ?: "Error al cerrar sesión"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                "Error al cerrar sesión: ${e.message}",
                e
            )
        }
    }

    override suspend fun sendTestMessage(phone: String, message: String): Result<String> {
        return try {
            val response = api.sendMessage(SendMessageRequest(phone, message))

            if (response.isSuccessful && response.body()?.success == true) {
                val messageId = response.body()!!.data?.id ?: "unknown"
                Result.Success(messageId)
            } else {
                Result.Error(
                    response.body()?.error ?: "Error al enviar mensaje"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                "Error al enviar mensaje: ${e.message}",
                e
            )
        }
    }

    override suspend fun sendMessage(phone: String, message: String): Result<String> {
        return sendTestMessage(phone, message)
    }

    override suspend fun sendImage(phone: String, imageBytes: ByteArray, caption: String): Result<String> {
        return try {
            val imgBody = imageBytes.toRequestBody("image/*".toMediaType())
            val part = MultipartBody.Part.createFormData("image", "image.jpg", imgBody)
            val phoneBody = phone.toRequestBody(MultipartBody.FORM)
            val captionBody = if (caption.isNotEmpty()) caption.toRequestBody(MultipartBody.FORM) else null
            val response = api.sendImage(phone = phoneBody, caption = captionBody, image = part)

            if (response.isSuccessful && response.body()?.success == true) {
                val messageId = response.body()!!.data?.id ?: "unknown"
                Result.Success(messageId)
            } else {
                Result.Error(
                    response.body()?.error ?: "Error al enviar imagen"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                "Error al enviar imagen: ${e.message}",
                e
            )
        }
    }

    /**
     * Mapea el estado del servidor al enum de la app
     */
    private fun mapStatus(status: String): SessionStatus {
        return when (status.lowercase()) {
            "disconnected" -> SessionStatus.DISCONNECTED
            "connecting" -> SessionStatus.CONNECTING
            "qr_ready" -> SessionStatus.QR_READY
            "link_code_ready" -> SessionStatus.LINK_CODE_READY
            "connected" -> SessionStatus.CONNECTED
            "failed" -> SessionStatus.FAILED
            else -> SessionStatus.DISCONNECTED
        }
    }
}
