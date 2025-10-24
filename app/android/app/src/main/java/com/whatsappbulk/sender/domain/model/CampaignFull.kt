package com.whatsappbulk.sender.domain.model

data class CampaignFull(
    val summary: CampaignSummary,
    val contacts: List<CampaignContact>
) {
    val totalContacts: Int get() = contacts.size
    val messages: List<String> get() = listOfNotNull(summary.mensaje1, summary.mensaje2, summary.mensaje3)
}

