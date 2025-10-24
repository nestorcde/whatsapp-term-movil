package com.whatsappbulk.sender.data.remote.dto

import com.google.gson.annotations.SerializedName

// Response genérico de la API
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: T?,
    @SerializedName("error")
    val error: String?
)

// DTOs para Auth/Session
data class SessionStatusDto(
    @SerializedName("status")
    val status: String,
    @SerializedName("qrCode")
    val qrCode: String?,
    @SerializedName("qrCodeBase64")
    val qrCodeBase64: String?,
    @SerializedName("linkCode")
    val linkCode: String?,
    @SerializedName("connectedPhone")
    val connectedPhone: String?,
    @SerializedName("isConnected")
    val isConnected: Boolean = false,
    @SerializedName("usePhoneNumber")
    val usePhoneNumber: Boolean = false
)

data class StartSessionRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null
)

data class LinkCodeDto(
    @SerializedName("linkCode")
    val linkCode: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("phoneNumber")
    val phoneNumber: Boolean
)

data class SendMessageRequest(
    @SerializedName("phone")
    val phone: String,
    @SerializedName("message")
    val message: String
)

data class MessageDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("ack")
    val ack: Int
)
