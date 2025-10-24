package com.whatsappbulk.sender.domain.model

data class CampaignConfig(
    val segundosDesde: Int,
    val segundosHasta: Int,
    val cantidadMaxDia: Int
)

data class Campaign(
    val id: Int,
    val oficina: String,
    val descripcion: String,
    val mensaje: String,
    val mensaje2: String?,
    val mensaje3: String?,
    val tieneImagen: Boolean,
    val tieneImagen2: Boolean,
    val tieneImagen3: Boolean,
    val fechaCreacion: String,
    val totales: CampaignTotals,
    val config: CampaignConfig?
) {
    val cantidadMensajes: Int
        get() = listOfNotNull(mensaje, mensaje2, mensaje3).count { it.isNotBlank() }

    val cantidadImagenes: Int
        get() = listOf(tieneImagen, tieneImagen2, tieneImagen3).count { it }
}

data class CampaignTotals(
    val total: Int,
    val pendientes: Int,
    val enviados: Int,
    val fallidos: Int
) {
    val percentage: Float
        get() = if (total > 0) enviados.toFloat() / total else 0f
}

data class CampaignDetail(
    val secuencia: Int,
    val socioNumero: String,
    val nombre: String,
    val telefono: String,
    val mensajeIndividual: String?,
    val estado: String?, // PEN, ENV, FAL
    val fechaHoraEstado: String?,
    val contadorIntentos: Int
) {
    val isPendiente: Boolean get() = estado == null || estado == "PEN"
    val isEnviado: Boolean get() = estado == "ENV"
    val isFallido: Boolean get() = estado == "FAL"
}
