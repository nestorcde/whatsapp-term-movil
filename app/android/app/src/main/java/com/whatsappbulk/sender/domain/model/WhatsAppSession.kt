package com.whatsappbulk.sender.domain.model

data class WhatsAppSession(
    val phoneNumber: String? = null,
    val linkCode: String? = null,
    val status: SessionStatus,
    val batteryLevel: Int? = null,
    val connectedAt: Long? = null
)

enum class SessionStatus {
    DISCONNECTED,
    CONNECTING,
    QR_READY,
    LINK_CODE_READY,
    CONNECTED,
    FAILED
}
