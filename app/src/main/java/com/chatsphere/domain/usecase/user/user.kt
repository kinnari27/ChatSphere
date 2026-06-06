// ────────────────────────────────────────────────────────────────

package com.chatsphere.domain.usecase.user

import com.chatsphere.domain.model.User
import com.chatsphere.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Searches for users by query string (for starting new chats or adding to groups).
 */
class SearchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(query: String): Result<List<User>> {
        if (query.length < 2) return Result.success(emptyList())
        return userRepository.searchUsers(query.trim())
    }
}

/**
 * Blocks a user from sending messages.
 */
class BlockUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> =
        userRepository.blockUser(userId)
}

/**
 * Updates the current user's profile information.
 */
class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(displayName: String, bio: String?): Result<User> {
        if (displayName.isBlank()) return Result.failure(IllegalArgumentException("Display name cannot be empty"))
        return userRepository.updateProfile(displayName.trim(), bio?.trim())
    }
}