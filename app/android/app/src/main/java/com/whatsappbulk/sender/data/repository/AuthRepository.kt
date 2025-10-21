package com.whatsappbulk.sender.data.repository

import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.model.User
import com.whatsappbulk.sender.domain.repository.IAuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() : IAuthRepository {

    // Usuario mock para testing
    private val mockUser = User(
        id = "1",
        username = "admin",
        token = "mock_token_12345"
    )

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            // Simular latencia de red
            delay(1500)

            // Validación mock
            if (username == "admin" && password == "admin") {
                Result.Success(mockUser)
            } else {
                Result.Error("Usuario o contraseña incorrectos")
            }
        } catch (e: Exception) {
            Result.Error("Error de conexión: ${e.message}", e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            delay(500)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Error al cerrar sesión: ${e.message}", e)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        // TODO: Implementar verificación real con DataStore/SharedPreferences
        return false
    }

    override suspend fun getCurrentUser(): User? {
        // TODO: Implementar obtención de usuario guardado desde DataStore/SharedPreferences
        return null
    }

    override suspend fun saveUser(user: User) {
        // TODO: Implementar guardado de usuario en DataStore/SharedPreferences
    }

    override suspend fun clearUser() {
        // TODO: Implementar limpieza de usuario guardado
    }
}
