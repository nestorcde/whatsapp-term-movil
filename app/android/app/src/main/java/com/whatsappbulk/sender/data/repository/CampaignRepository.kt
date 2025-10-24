package com.whatsappbulk.sender.data.repository

import com.whatsappbulk.sender.data.remote.api.CampaignApi
import com.whatsappbulk.sender.data.remote.dto.UpdateMessageStatusRequest
import com.whatsappbulk.sender.domain.model.Campaign
import com.whatsappbulk.sender.domain.model.CampaignConfig
import com.whatsappbulk.sender.domain.model.CampaignDetail
import com.whatsappbulk.sender.domain.model.CampaignTotals
import com.whatsappbulk.sender.domain.model.CampaignContact
import com.whatsappbulk.sender.domain.model.CampaignFull
import com.whatsappbulk.sender.domain.model.CampaignSummary
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.repository.ICampaignRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepository @Inject constructor(
    private val campaignApi: CampaignApi
) : ICampaignRepository {

    override suspend fun getCampaigns(): Result<List<Campaign>> {
        return try {
            val response = campaignApi.getCampaigns()

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val campaigns = body.data.map { dto ->

                        // 1) Resolver totales desde el objeto o desde los *_Contactos
                        val total = dto.totales?.total ?: dto.totalContactos ?: 0
                        val enviados = dto.totales?.enviados ?: dto.enviadosContactos ?: 0
                        val pendientes = dto.totales?.pendientes ?: dto.pendientesContactos ?: 0
                        val fallidos = dto.totales?.fallidos ?: dto.fallidosContactos ?: 0

                        // 2) Defaults para campos ausentes en el JSON real
                        val oficina = dto.oficina ?: "N/D"
                        val descripcion = dto.descripcion ?: dto.mensaje ?: "(sin descripción)"
                        val fechaCreacion = dto.fechaCreacion ?: dto.fecha ?: ""

                        // 3) Flags de imágenes con default seguro
                        val img1 = dto.tieneImagen ?: false
                        val img2 = dto.tieneImagen2 ?: false
                        val img3 = dto.tieneImagen3 ?: false


                        Campaign(
                            id = dto.id,
                            oficina = oficina,
                            descripcion = descripcion,
                            mensaje = dto.mensaje ?: "",
                            mensaje2 = dto.mensaje2,
                            mensaje3 = dto.mensaje3,
                            tieneImagen = img1,
                            tieneImagen2 = img2,
                            tieneImagen3 = img3,
                            fechaCreacion = fechaCreacion,
                            totales = CampaignTotals(
                                total = total,
                                pendientes = pendientes,
                                enviados = enviados,
                                fallidos = fallidos
                            ),
                            config = dto.config?.let {
                                CampaignConfig(
                                    segundosDesde = it.segundosDesde,
                                    segundosHasta = it.segundosHasta,
                                    cantidadMaxDia = it.cantidadMaxDia
                                )
                            }
                        )
                    }
                    Result.Success(campaigns)
                } else {
                    Result.Error(body?.error ?: "Error desconocido")
                }
            } else {
                Result.Error("Error de red: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener campañas")
        }
    }

    override suspend fun getCampaignDetails(
        campaignId: Int,
        limit: Int?
    ): Result<List<CampaignDetail>> {
        return try {
            val response = campaignApi.getCampaignDetails(campaignId, limit)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val details = body.data.map { dto ->
                        CampaignDetail(
                            secuencia = dto.secuencia,
                            socioNumero = dto.socioNumero,
                            nombre = dto.nombre,
                            telefono = dto.telefono,
                            mensajeIndividual = dto.mensajeIndividual,
                            estado = dto.estado,
                            fechaHoraEstado = dto.fechaHoraEstado,
                            contadorIntentos = dto.contadorIntentos
                        )
                    }
                    Result.Success(details)
                } else {
                    Result.Error(body?.error ?: "Error desconocido")
                }
            } else {
                Result.Error("Error de red: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener detalles de campaña")
        }
    }

    override suspend fun getCampaignImage(
        campaignId: Int,
        imageNumber: Int
    ): Result<ByteArray?> {
        return try {
            val response = campaignApi.getCampaignImage(campaignId, imageNumber)

            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                Result.Success(bytes)
            } else if (response.code() == 404) {
                // La imagen no existe
                Result.Success(null)
            } else {
                Result.Error("Error al obtener imagen: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener imagen")
        }
    }

    override suspend fun updateMessageStatus(
        campaignId: Int,
        secuencia: Int,
        estado: String
    ): Result<Unit> {
        return try {
            val request = UpdateMessageStatusRequest(
                campaignId = campaignId,
                secuencia = secuencia,
                estado = estado
            )

            val response = campaignApi.updateMessageStatus(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.Success(Unit)
                } else {
                    Result.Error(body?.error ?: "Error desconocido")
                }
            } else {
                Result.Error("Error de red: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al actualizar estado")
        }
    }

    // ============ NUEVOS MÉTODOS PARA 4.2 ============
    override suspend fun getCampaignSummaryList(): Result<List<CampaignSummary>> {
        return try {
            val response = campaignApi.getCampaigns()

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val campaigns = body.data.map { dto ->
                        val total = dto.totales?.total ?: dto.totalContactos ?: 0
                        val enviados = dto.totales?.enviados ?: dto.enviadosContactos ?: 0
                        val pendientes = dto.totales?.pendientes ?: dto.pendientesContactos ?: 0
                        val fallidos = dto.totales?.fallidos ?: dto.fallidosContactos ?: 0

                        val oficina = dto.oficina ?: "N/D"
                        val descripcion = dto.descripcion ?: dto.mensaje ?: "(sin descripción)"
                        val fecha = dto.fechaCreacion ?: dto.fecha ?: ""

                        CampaignSummary(
                            id = dto.id,
                            oficina = oficina,
                            descripcion = descripcion,
                            mensaje1 = dto.mensaje,
                            mensaje2 = dto.mensaje2,
                            mensaje3 = dto.mensaje3,
                            tieneImagen1 = dto.tieneImagen ?: false,
                            tieneImagen2 = dto.tieneImagen2 ?: false,
                            tieneImagen3 = dto.tieneImagen3 ?: false,
                            fecha = fecha,
                            totales = CampaignTotals(
                                total = total,
                                pendientes = pendientes,
                                enviados = enviados,
                                fallidos = fallidos
                            ),
                            config = dto.config?.let {
                                CampaignConfig(
                                    segundosDesde = it.segundosDesde,
                                    segundosHasta = it.segundosHasta,
                                    cantidadMaxDia = it.cantidadMaxDia
                                )
                            }
                        )
                    }
                    Result.Success(campaigns)
                } else {
                    Result.Error(body?.error ?: "Error desconocido")
                }
            } else {
                Result.Error("Error de red: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener campañas")
        }
    }

    override suspend fun getCampaignFull(campaignId: Int, limit: Int?): Result<CampaignFull> {
        return try {
            // Obtener summary desde listado
            val listResult = getCampaignSummaryList()
            if (listResult !is Result.Success) return Result.Error((listResult as? Result.Error)?.message ?: "Error al obtener campañas")
            val summary = listResult.data.firstOrNull { it.id == campaignId }
                ?: return Result.Error("Campaña no encontrada")

            // Obtener detalles/contacts
            val response = campaignApi.getCampaignDetails(campaignId, limit)
            if (!response.isSuccessful) return Result.Error("Error de red: ${response.code()}")

            val body = response.body()
            if (body?.success == true && body.data != null) {
                val contacts = body.data.map { dto ->
                    CampaignContact(
                        secuencia = dto.secuencia,
                        nombre = dto.nombre,
                        telefono = dto.telefono,
                        estado = dto.estado,
                        mensajeIndividual = dto.mensajeIndividual,
                        fechaHoraEstado = dto.fechaHoraEstado,
                        contadorIntentos = dto.contadorIntentos
                    )
                }
                Result.Success(CampaignFull(summary = summary, contacts = contacts))
            } else {
                Result.Error(body?.error ?: "Error desconocido")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al obtener detalles de campaña")
        }
    }
}
