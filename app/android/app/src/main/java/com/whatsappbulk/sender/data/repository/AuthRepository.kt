package com.whatsappbulk.sender.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.whatsappbulk.sender.data.remote.api.CampaignApi
import com.whatsappbulk.sender.data.remote.dto.LoginRequest
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.model.User
import com.whatsappbulk.sender.domain.repository.IAuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val campaignApi: CampaignApi
) : IAuthRepository {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id"
    }

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            val response = campaignApi.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.data?.token
                val userDto = body?.data?.user
                if (body?.success == true && !token.isNullOrEmpty() && userDto != null) {
                    val user = User(
                        id = userDto.username,
                        username = userDto.username,
                        token = token
                    )
                    saveUser(user)
                    Result.Success(user)
                } else {
                    Result.Error(body?.error ?: body?.message ?: "Error de autenticación")
                }
            } else {
                val code = response.code()
                val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                val message = when (code) {
                    401, 403 -> "Error de autenticación: usuario o contraseña incorrectos"
                    400 -> "Solicitud inválida"
                    else -> null
                }
                Result.Error(message ?: (errorBody ?: "Error de red: $code"))
            }
        } catch (e: Exception) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            clearUser()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Error al cerrar sesión: ${e.message}", e)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return !encryptedPrefs.getString(KEY_TOKEN, null).isNullOrEmpty()
    }

    override suspend fun getCurrentUser(): User? {
        val token = encryptedPrefs.getString(KEY_TOKEN, null) ?: return null
        val username = encryptedPrefs.getString(KEY_USERNAME, null) ?: return null
        val userId = encryptedPrefs.getString(KEY_USER_ID, null) ?: username
        return User(
            id = userId,
            username = username,
            token = token
        )
    }

    override suspend fun saveUser(user: User) {
        encryptedPrefs.edit().apply {
            putString(KEY_TOKEN, user.token)
            putString(KEY_USERNAME, user.username)
            putString(KEY_USER_ID, user.id)
            apply()
        }
    }

    override suspend fun clearUser() {
        encryptedPrefs.edit().clear().apply()
    }
}
