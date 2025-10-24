package com.whatsappbulk.sender.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs para la API de campañas del backend Oracle
 */

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,   // presente en éxito
    @SerializedName("error")
    val error: String? = null,     // presente en error
    @SerializedName("data")
    val data: LoginData? = null    // presente en éxito
)

data class LoginData(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("user")
    val user: UserDto? = null
)

data class UserDto(
    @SerializedName("username")
    val username: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("sucursal")
    val sucursal: Int
)


data class CampaignConfigDto(
    @SerializedName("segundosDesde")
    val segundosDesde: Int,
    @SerializedName("segundosHasta")
    val segundosHasta: Int,
    @SerializedName("cantidadMaxDia")
    val cantidadMaxDia: Int
)

data class CampaignDto(
    @SerializedName("id")
    val id: Int,

    // Estos pueden no venir en tu JSON → hácelos opcionales
    @SerializedName("oficina")
    val oficina: String? = null,
    @SerializedName("descripcion")
    val descripcion: String? = null,

    // Mensajes
    @SerializedName("mensaje")
    val mensaje: String? = null,
    @SerializedName("mensaje2")
    val mensaje2: String? = null,
    @SerializedName("mensaje3")
    val mensaje3: String? = null,

    // Flags de imágenes
    @SerializedName("tieneImagen")
    val tieneImagen: Boolean? = null,
    @SerializedName("tieneImagen2")
    val tieneImagen2: Boolean? = null,
    @SerializedName("tieneImagen3")
    val tieneImagen3: Boolean? = null,

    // Tu backend envía "fecha" (no "fechaCreacion")
    @SerializedName("fecha")
    val fecha: String? = null,
    @SerializedName("fechaCreacion")
    val fechaCreacion: String? = null, // por si en otro endpoint sí viene

    // A veces el backend puede traer un objeto "totales"…
    @SerializedName("totales")
    val totales: CampaignTotalsDto? = null,

    // …pero en tu log llegan campos sueltos:
    @SerializedName("totalContactos")
    val totalContactos: Int? = null,
    @SerializedName("enviadosContactos")
    val enviadosContactos: Int? = null,
    @SerializedName("pendientesContactos")
    val pendientesContactos: Int? = null,
    @SerializedName("fallidosContactos")
    val fallidosContactos: Int? = null,

    // Configuración de envío desde GENTN000
    @SerializedName("config")
    val config: CampaignConfigDto? = null
)

data class CampaignTotalsDto(
    @SerializedName("total")
    val total: Int? = null,
    @SerializedName("pendientes")
    val pendientes: Int? = null,
    @SerializedName("enviados")
    val enviados: Int? = null,
    @SerializedName("fallidos")
    val fallidos: Int? = null
)

data class CampaignDetailDto(
    @SerializedName("secuencia")
    val secuencia: Int,
    @SerializedName("socioNumero")
    val socioNumero: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("telefono")
    val telefono: String,
    @SerializedName("mensajeIndividual")
    val mensajeIndividual: String?,
    @SerializedName("estado")
    val estado: String?, // PEN, ENV, ERR
    @SerializedName("fechaHoraEstado")
    val fechaHoraEstado: String?,
    @SerializedName("contadorIntentos")
    val contadorIntentos: Int
)

data class UpdateMessageStatusRequest(
    @SerializedName("campaignId")
    val campaignId: Int,
    @SerializedName("secuencia")
    val secuencia: Int,
    @SerializedName("estado")
    val estado: String // PEN, ENV, ERR
)
