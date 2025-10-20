package com.whatsappbulk.sender.domain.repository

import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.model.User

interface IAuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUser(): User?
    suspend fun saveUser(user: User)
    suspend fun clearUser()
}
