package com.whatsappbulk.sender.domain.repository

import com.whatsappbulk.sender.domain.model.Campaign
import com.whatsappbulk.sender.domain.model.CampaignDetail
import com.whatsappbulk.sender.domain.model.Result

/**
 * Interfaz del repositorio para operaciones con campañas
 */
interface ICampaignRepository {

    /**
     * Obtener lista de campañas disponibles para el usuario autenticado
     */
    suspend fun getCampaigns(): Result<List<Campaign>>

    /**
     * Obtener detalles de una campaña específica (lista de contactos)
     * @param campaignId ID de la campaña
     * @param limit Límite de contactos a cargar (null = todos)
     */
    suspend fun getCampaignDetails(campaignId: Int, limit: Int? = null): Result<List<CampaignDetail>>

    /**
     * Obtener imagen de una campaña
     * @param campaignId ID de la campaña
     * @param imageNumber Número de imagen (1, 2 o 3)
     * @return ByteArray de la imagen o null si no existe
     */
    suspend fun getCampaignImage(campaignId: Int, imageNumber: Int): Result<ByteArray?>

    /**
     * Actualizar estado de un mensaje enviado
     * @param campaignId ID de la campaña
     * @param secuencia Secuencia del mensaje
     * @param estado Estado nuevo (PEN, ENV, FAL)
     */
    suspend fun updateMessageStatus(campaignId: Int, secuencia: Int, estado: String): Result<Unit>

    // Nuevo flujo 4.2: Summary & Full
    suspend fun getCampaignSummaryList(): Result<List<com.whatsappbulk.sender.domain.model.CampaignSummary>>
    suspend fun getCampaignFull(campaignId: Int, limit: Int? = null): Result<com.whatsappbulk.sender.domain.model.CampaignFull>
}
