package com.whatsappbulk.sender.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappbulk.sender.domain.model.Result
import com.whatsappbulk.sender.domain.repository.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(
            username = username,
            usernameError = null,
            errorMessage = null
        ) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(
            password = password,
            passwordError = null,
            errorMessage = null
        ) }
    }

    fun onRememberMeChange(rememberMe: Boolean) {
        _uiState.update { it.copy(rememberMe = rememberMe) }
    }

    fun login() {
        // Validaciones
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null
            ) }

            when (val result = authRepository.login(
                username = _uiState.value.username.trim(),
                password = _uiState.value.password
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        loginSuccess = true,
                        errorMessage = null
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        loginSuccess = false,
                        errorMessage = result.message
                    ) }
                }
                is Result.Loading -> {
                    // Ya está en loading
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val username = _uiState.value.username.trim()
        val password = _uiState.value.password

        var isValid = true
        var usernameError: String? = null
        var passwordError: String? = null

        // Validar usuario
        if (username.isBlank()) {
            usernameError = "El usuario es requerido"
            isValid = false
        } else if (username.length < 3) {
            usernameError = "El usuario debe tener al menos 3 caracteres"
            isValid = false
        }

        // Validar contraseña
        if (password.isBlank()) {
            passwordError = "La contraseña es requerida"
            isValid = false
        } else if (password.length < 4) {
            passwordError = "La contraseña debe tener al menos 4 caracteres"
            isValid = false
        }

        _uiState.update { it.copy(
            usernameError = usernameError,
            passwordError = passwordError
        ) }

        return isValid
    }

    fun resetLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }
}
