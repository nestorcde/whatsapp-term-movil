package com.whatsappbulk.sender.domain.model

data class CampaignContact(
    val secuencia: Int,
    val numeroSocio: Int,
    val nombre: String,
    val telefono: String,
    val estado: String?,
    val mensajeIndividual: String?,
    val fechaHoraEstado: String?,
    val contadorIntentos: Int
)

