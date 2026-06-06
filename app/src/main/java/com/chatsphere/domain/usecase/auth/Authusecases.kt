package com.chatsphere.domain.usecase.auth

import com.chatsphere.domain.model.AuthSession
import com.chatsphere.domain.model.LoginCredentials
import com.chatsphere.domain.model.RegisterData
import com.chatsphere.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Validates and executes user login.
 *
 * Follows the single-responsibility principle — only concerned with
 * credential validation and delegating to the repository.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email cannot be empty"))
        if (!email.contains("@")) return Result.failure(IllegalArgumentException("Invalid email format"))
        if (password.length < 6) return Result.failure(IllegalArgumentException("Password too short"))

        return authRepository.login(LoginCredentials(email.trim(), password))
    }
}

/**
 * Validates registration data and creates a new account.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        username: String,
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Result<AuthSession> {
        if (username.isBlank()) return Result.failure(IllegalArgumentException("Username cannot be empty"))
        if (username.length < 3) return Result.failure(IllegalArgumentException("Username must be at least 3 characters"))
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$")))
            return Result.failure(IllegalArgumentException("Username can only contain letters, numbers, and underscores"))
        if (displayName.isBlank()) return Result.failure(IllegalArgumentException("Display name cannot be empty"))
        if (!email.contains("@")) return Result.failure(IllegalArgumentException("Invalid email format"))
        if (password.length < 8) return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        if (password != confirmPassword) return Result.failure(IllegalArgumentException("Passwords do not match"))

        return authRepository.register(
            RegisterData(
                username = username.trim(),
                displayName = displayName.trim(),
                email = email.trim(),
                password = password
            )
        )
    }
}

/**
 * Clears local session and logs out from the server.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}

/**
 * Observes authentication state changes for app-level routing.
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke() = authRepository.observeSession()
}