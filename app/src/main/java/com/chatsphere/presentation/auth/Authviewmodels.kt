package com.chatsphere.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatsphere.domain.usecase.auth.LoginUseCase
import com.chatsphere.domain.usecase.auth.RegisterUseCase
import com.chatsphere.presentation.UiEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────── Login ───────────────────────────────────────────

/**
 * Manages login form state and triggers authentication.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects: Flow<UiEffect> = _effects.receiveAsFlow()

    fun onEmailChanged(email: String) = _uiState.update { it.copy(email = email, emailError = null) }
    fun onPasswordChanged(password: String) = _uiState.update { it.copy(password = password, passwordError = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun login() {
        val state = _uiState.value
        if (!validate(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            loginUseCase(state.email, state.password)
                .onSuccess {
                    _effects.send(UiEffect.NavigateTo("home"))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, generalError = error.message) }
                }
        }
    }

    private fun validate(state: LoginUiState): Boolean {
        var isValid = true
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            isValid = false
        }
        return isValid
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
)

// ─────────────────── Register ────────────────────────────────────────

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects: Flow<UiEffect> = _effects.receiveAsFlow()

    fun onUsernameChanged(v: String) = _uiState.update { it.copy(username = v, usernameError = null) }
    fun onDisplayNameChanged(v: String) = _uiState.update { it.copy(displayName = v, displayNameError = null) }
    fun onEmailChanged(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onPasswordChanged(v: String) = _uiState.update { it.copy(password = v, passwordError = null) }
    fun onConfirmPasswordChanged(v: String) = _uiState.update { it.copy(confirmPassword = v, confirmPasswordError = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun register() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            registerUseCase(
                username = state.username,
                displayName = state.displayName,
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword
            )
                .onSuccess { _effects.send(UiEffect.NavigateTo("home")) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = error.message)
                    }
                }
        }
    }
}

data class RegisterUiState(
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null
)