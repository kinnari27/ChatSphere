package com.chatsphere.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatsphere.core.common.AppResult
import com.chatsphere.core.common.UiEvent
import com.chatsphere.core.common.UiState
import com.chatsphere.domain.usecase.LoginUseCase
import com.chatsphere.domain.usecase.LoginWithGoogleUseCase
import com.chatsphere.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
) : UiState

sealed interface AuthEvent : UiEvent {
    data class NameChanged(val value: String) : AuthEvent
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class LoginWithGoogle(val idToken: String) : AuthEvent
    data object Login : AuthEvent
    data object Register : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.NameChanged -> _state.update { it.copy(name = event.value) }
            is AuthEvent.EmailChanged -> _state.update { it.copy(email = event.value) }
            is AuthEvent.PasswordChanged -> _state.update { it.copy(password = event.value) }
            is AuthEvent.LoginWithGoogle -> submitGoogleLogin(event.idToken)
            AuthEvent.Login -> submitLogin()
            AuthEvent.Register -> submitRegister()
        }
    }

    private fun submitLogin() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = loginUseCase(state.value.email, state.value.password)) {
            is AppResult.Success -> _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = result.message.ifBlank { "Unable to sign in" }) }
            AppResult.Loading -> Unit
        }
    }

    private fun submitGoogleLogin(idToken: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = loginWithGoogleUseCase(idToken)) {
            is AppResult.Success -> _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = result.message.ifBlank { "Google sign in failed" }) }
            AppResult.Loading -> Unit
        }
    }

    private fun submitRegister() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = registerUseCase(state.value.name, state.value.email, state.value.password)) {
            is AppResult.Success -> _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            is AppResult.Error -> _state.update { it.copy(isLoading = false, error = result.message.ifBlank { "Unable to create account" }) }
            AppResult.Loading -> Unit
        }
    }
}
