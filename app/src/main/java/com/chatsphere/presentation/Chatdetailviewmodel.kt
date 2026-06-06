package com.chatsphere.presentation

/**
 * Generic wrapper for screen UI states.
 * Used across all ViewModels for consistent loading/error handling.
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>()
}

/**
 * One-shot side effects that the UI should react to once (navigation, snackbars, etc.)
 * Sealed to make the event contract explicit and exhaustive.
 */
sealed class UiEffect {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEffect()
    data class NavigateTo(val route: String) : UiEffect()
    object NavigateBack : UiEffect()
    data class ShowDialog(val title: String, val message: String) : UiEffect()
}


