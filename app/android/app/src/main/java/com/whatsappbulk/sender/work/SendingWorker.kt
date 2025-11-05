package com.whatsappbulk.sender.work

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.whatsappbulk.sender.data.remote.api.CampaignApi
import com.whatsappbulk.sender.data.remote.api.WhatsAppApi
import com.whatsappbulk.sender.data.repository.CampaignRepository
import com.whatsappbulk.sender.data.repository.WhatsAppRepository
import com.whatsappbulk.sender.domain.model.CampaignContact
import com.whatsappbulk.sender.domain.model.CampaignFull
import com.whatsappbulk.sender.domain.model.CampaignSummary
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.util.NotificationUtils
import kotlinx.coroutines.delay
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.random.Random
import com.whatsappbulk.sender.BuildConfig

class SendingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_CAMPAIGN_ID = "campaignId"
        const val KEY_QUANTITY = "quantity"
        const val NOTIF_ID = 1001
        private const val WHATSAPP_BASE_URL = "http://127.0.0.1:3000/"
        private const val CAMPAIGN_BASE_URL_UNUSED = "http://192.168.31.33:3001/" // deprecated, using BuildConfig
    }

    override suspend fun doWork(): Result {
        val campaignId = inputData.getInt(KEY_CAMPAIGN_ID, -1)
        val quantity = inputData.getInt(KEY_QUANTITY, 0)
        if (campaignId <= 0 || quantity <= 0) return Result.failure()

        setForeground(createForegroundInfo(0, quantity, "Preparando envío"))

        val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

        // Auth interceptor para Campaign API (token de EncryptedSharedPreferences)
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = encryptedPrefs.getString("jwt_token", null)
        val authInterceptor = Interceptor { chain ->
            val req = chain.request()
            val newReq = if (!token.isNullOrEmpty()) {
                req.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else req
            chain.proceed(newReq)
        }

        val whatsHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val campHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()

        val whatsRetrofit = Retrofit.Builder()
            .baseUrl(WHATSAPP_BASE_URL)
            .client(whatsHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val campRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(campHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val whatsappApi = whatsRetrofit.create(WhatsAppApi::class.java)
        val campaignApi = campRetrofit.create(CampaignApi::class.java)
        val whatsRepo = WhatsAppRepository(whatsappApi)
        val campRepo = CampaignRepository(campaignApi)

        // Obtener campaña completa
        val fullRes = campRepo.getCampaignFull(campaignId)
        if (fullRes !is com.whatsappbulk.sender.domain.model.Result.Success) return Result.failure()
        val full = fullRes.data
        val pendingContacts = full.contacts.filter { it.estado == null || it.estado == "PEN" }
        val available = pendingContacts.size
        val toSend = quantity.coerceAtMost(available).coerceAtMost(50)
        val targets = pendingContacts.take(toSend)

        // Pre-cargar imagenes una sola vez si existen
        val imageCache = hashMapOf<Int, ByteArray>()
        if (full.summary.tieneImagen1) {
            (campRepo.getCampaignImage(campaignId, 1) as? com.whatsappbulk.sender.domain.model.Result.Success)?.data?.let {
                imageCache[0] = it
            }
        }
        if (full.summary.tieneImagen2) {
            (campRepo.getCampaignImage(campaignId, 2) as? com.whatsappbulk.sender.domain.model.Result.Success)?.data?.let {
                imageCache[1] = it
            }
        }
        if (full.summary.tieneImagen3) {
            (campRepo.getCampaignImage(campaignId, 3) as? com.whatsappbulk.sender.domain.model.Result.Success)?.data?.let {
                imageCache[2] = it
            }
        }

        for ((index, contact) in targets.withIndex()) {
            if (isStopped) return Result.success()

            val progressText = "Enviando ${index + 1} de ${toSend}"
            setForeground(createForegroundInfo(index, toSend, progressText))

            val msgIndex = if (full.messages.isNotEmpty()) index % full.messages.size else 0
            val msg = "${contact.nombre} ${full.messages.getOrNull(msgIndex) ?: ""}"

            val sendResult = if (shouldSendImage(full, msgIndex)) {
                val imgBytes = imageCache[msgIndex]
                if (imgBytes != null) {
                    whatsRepo.sendImage(normalizePhone(contact.telefono), imgBytes, msg)
                } else {
                    whatsRepo.sendTestMessage(normalizePhone(contact.telefono), msg)
                }
            } else {
                whatsRepo.sendTestMessage(normalizePhone(contact.telefono), msg)
            }

            if (sendResult is com.whatsappbulk.sender.domain.model.Result.Success) {
                campRepo.updateMessageStatus(campaignId, contact.secuencia, "ENV")
            } else {
                campRepo.updateMessageStatus(campaignId, contact.secuencia, "FAL")
            }

            val isLast = index == targets.lastIndex
            if (!isLast) {
                val waitSec = Random.nextInt(20, 51)
                var remaining = waitSec
                while (remaining > 0 && !isStopped) {
                    delay(1000)
                    remaining--
                    setForeground(createForegroundInfo(index, toSend, "Siguiente en ${remaining}s"))
                }
            }
        }

        setForeground(createForegroundInfo(toSend, toSend, "Envío completado"))
        return Result.success()
    }

    private fun createForegroundInfo(progress: Int, max: Int, content: String): ForegroundInfo {
            val notification = NotificationUtils.buildProgressNotification(
            applicationContext,
            title = "Ejecución de campaña",
            content = content,
            progress = progress,
            max = max,
            ongoing = progress < max
        )
        // Especificar tipo DATA_SYNC para Android 14+
        return if (android.os.Build.VERSION.SDK_INT >= 34) {
            ForegroundInfo(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID, notification)
        }
    }

    private fun shouldSendImage(full: CampaignFull, msgIndex: Int): Boolean {
        return when (msgIndex) {
            0 -> full.summary.tieneImagen1
            1 -> full.summary.tieneImagen2
            2 -> full.summary.tieneImagen3
            else -> false
        }
    }

    private fun normalizePhone(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("@")) return trimmed
        return trimmed.filter { it.isDigit() }
    }
}
