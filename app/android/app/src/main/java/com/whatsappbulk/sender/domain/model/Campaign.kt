package com.whatsappbulk.sender.domain.model

data class Campaign(
    val id: String,
    val name: String,
    val messages: List<String>,
    val contacts: List<String>,
    val delayRange: DelayRange,
    val dailyQuota: DailyQuota,
    val createdAt: Long
)

data class DelayRange(
    val min: Int,  // segundos
    val max: Int   // segundos
)

data class DailyQuota(
    val total: Int,
    val used: Int,
    val remaining: Int
) {
    val percentage: Float
        get() = if (total > 0) used.toFloat() / total else 0f
}
