package com.whatsappbulk.sender.domain.model

data class CampaignSummary(
    val id: Int,
    val oficina: String,
    val descripcion: String,
    val mensaje1: String?,
    val mensaje2: String?,
    val mensaje3: String?,
    val tieneImagen1: Boolean,
    val tieneImagen2: Boolean,
    val tieneImagen3: Boolean,
    val fecha: String,
    val totales: CampaignTotals,
    val config: CampaignConfig?
)

