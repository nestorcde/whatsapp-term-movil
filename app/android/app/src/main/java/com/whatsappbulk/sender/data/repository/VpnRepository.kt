package com.whatsappbulk.sender.data.repository

import android.content.Context
import android.content.Intent
import com.whatsappbulk.sender.data.remote.api.HealthApi
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.repository.IVpnRepository
import com.whatsappbulk.sender.util.VpnHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthApi: HealthApi
) : IVpnRepository {

    override suspend fun checkBackendHealth(): Result<Boolean> {
        return try {
            val response = healthApi.checkHealth()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) Result.Success(true)
                else Result.Error(body?.message ?: "Health check falló")
            } else {
                Result.Error("Error de red: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    override fun isFortiClientInstalled(): Boolean {
        return VpnHelper.isAppInstalled(context, VpnHelper.FORTICLIENT_PACKAGE)
    }

    override fun launchFortiClient(): Result<Unit> {
        return try {
            val intent: Intent? = VpnHelper.launchApp(context, VpnHelper.FORTICLIENT_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result.Success(Unit)
            } else {
                Result.Error("No se pudo abrir FortiClient")
            }
        } catch (e: Exception) {
            Result.Error("No se pudo abrir FortiClient: ${e.message}", e)
        }
    }

    override fun openPlayStoreFortiClient(): Result<Unit> {
        return try {
            val intent: Intent = VpnHelper.openPlayStore(context, VpnHelper.FORTICLIENT_PACKAGE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("No se pudo abrir Play Store: ${e.message}", e)
        }
    }
}

