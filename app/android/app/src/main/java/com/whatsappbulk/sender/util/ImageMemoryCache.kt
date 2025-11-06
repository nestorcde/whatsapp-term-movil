package com.whatsappbulk.sender.util

object ImageMemoryCache {
    // Key: Pair(campaignId, imageNumber[1..3]) → bytes
    private val cache = HashMap<Pair<Int, Int>, ByteArray>()

    fun put(campaignId: Int, imageNumber: Int, bytes: ByteArray) {
        cache[Pair(campaignId, imageNumber)] = bytes
    }

    fun get(campaignId: Int, imageNumber: Int): ByteArray? {
        return cache[Pair(campaignId, imageNumber)]
    }

    fun clearCampaign(campaignId: Int) {
        val keys = cache.keys.filter { it.first == campaignId }
        keys.forEach { cache.remove(it) }
    }
}

