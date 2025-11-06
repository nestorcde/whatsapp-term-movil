package com.whatsappbulk.sender.domain.model

data class User(
    val id: String,
    val username: String,
    val token: String,
    val createdAt: Long = System.currentTimeMillis()
)
