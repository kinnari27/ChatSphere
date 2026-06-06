package com.chatsphere.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val throwable: Throwable, val message: String = throwable.message.orEmpty()) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}
