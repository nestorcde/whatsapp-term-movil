package com.whatsappbulk.sender.domain.model

data class Execution(
    val id: String,
    val campaignId: String,
    val campaignName: String,
    val quantity: Int,
    val status: ExecutionStatus,
    val progress: ExecutionProgress,
    val currentMessage: CurrentMessage? = null,
    val startedAt: Long,
    val completedAt: Long? = null
)

enum class ExecutionStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}

data class ExecutionProgress(
    val total: Int,
    val sent: Int,
    val failed: Int,
    val pending: Int
) {
    val percentage: Float
        get() = if (total > 0) sent.toFloat() / total else 0f
}

data class CurrentMessage(
    val contact: String,
    val message: String,
    val timestamp: Long
)
