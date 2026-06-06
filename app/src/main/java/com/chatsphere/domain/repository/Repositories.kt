package com.chatsphere.domain.repository

import com.chatsphere.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Contract for authentication operations.
 * Implementations live in the data layer.
 */
interface AuthRepository {
    /** Authenticate with email/password and persist the session. */
    suspend fun login(credentials: LoginCredentials): Result<AuthSession>

    /** Create a new account and return the session. */
    suspend fun register(data: RegisterData): Result<AuthSession>

    /** Log out and clear the local session. */
    suspend fun logout(): Result<Unit>

    /** Attempt to refresh the JWT if expired. */
    suspend fun refreshToken(): Result<AuthSession>

    /** Observe the current session (null when logged out). */
    fun observeSession(): Flow<AuthSession?>

    /** Synchronously check if a valid session exists. */
    fun isAuthenticated(): Boolean

    /** Upload a device FCM token to the server. */
    suspend fun updateFcmToken(token: String): Result<Unit>
}

/**
 * Contract for fetching and sending chat messages.
 * Offline-first: local cache is the source of truth.
 */
interface ChatRepository {
    /** Observe all conversations for the current user. */
    fun observeChats(): Flow<List<Chat>>

    /** Observe a single conversation by ID. */
    fun observeChat(chatId: String): Flow<Chat?>

    /** Paginate messages for a conversation. */
    fun observeMessages(chatId: String, pageSize: Int = 30): Flow<List<Message>>

    /** Send a message (queued locally first, then synced). */
    suspend fun sendMessage(chatId: String, content: MessageContent): Result<Message>

    /** Delete a message (own messages only). */
    suspend fun deleteMessage(messageId: String): Result<Unit>

    /** Edit the text content of a message. */
    suspend fun editMessage(messageId: String, newText: String): Result<Message>

    /** Mark all messages in a chat as read. */
    suspend fun markChatAsRead(chatId: String): Result<Unit>

    /** Pin a message for visibility. */
    suspend fun pinMessage(messageId: String): Result<Unit>

    /** Toggle archive state for a conversation. */
    suspend fun archiveChat(chatId: String, archive: Boolean): Result<Unit>

    /** Search messages by keyword across all chats. */
    suspend fun searchMessages(query: String): Result<List<Message>>

    /** Add a reaction emoji to a message. */
    suspend fun reactToMessage(messageId: String, emoji: String): Result<Unit>

    /** Sync any pending (offline-queued) messages. */
    suspend fun syncPendingMessages(): Result<Unit>
}

/**
 * Contract for group management operations.
 */
interface GroupRepository {
    /** Observe all groups the user belongs to. */
    fun observeGroups(): Flow<List<Group>>

    /** Fetch detailed info about a group. */
    suspend fun getGroup(groupId: String): Result<Group>

    /** Create a new group with initial members. */
    suspend fun createGroup(name: String, memberIds: List<String>, avatarUrl: String?): Result<Group>

    /** Add members to an existing group (admin only). */
    suspend fun addMembers(groupId: String, memberIds: List<String>): Result<Unit>

    /** Remove a member from the group (admin only). */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>

    /** Leave the group (self-removal). */
    suspend fun leaveGroup(groupId: String): Result<Unit>

    /** Update the group name or description. */
    suspend fun updateGroupInfo(groupId: String, name: String, description: String?): Result<Group>

    /** Upload a group avatar image. */
    suspend fun updateGroupAvatar(groupId: String, imageUri: String): Result<String>
}

/**
 * Contract for user profile and social operations.
 */
interface UserRepository {
    /** Fetch the logged-in user's profile. */
    suspend fun getCurrentUser(): Result<User>

    /** Observe the current user for reactive UI updates. */
    fun observeCurrentUser(): Flow<User?>

    /** Search users by display name or username. */
    suspend fun searchUsers(query: String): Result<List<User>>

    /** Fetch a user profile by ID. */
    suspend fun getUserById(userId: String): Result<User>

    /** Update display name, bio, or avatar. */
    suspend fun updateProfile(displayName: String, bio: String?): Result<User>

    /** Upload and set a new avatar. */
    suspend fun updateAvatar(imageUri: String): Result<String>

    /** Block a user from messaging you. */
    suspend fun blockUser(userId: String): Result<Unit>

    /** Unblock a previously blocked user. */
    suspend fun unblockUser(userId: String): Result<Unit>

    /** Fetch all users blocked by the current user. */
    suspend fun getBlockedUsers(): Result<List<User>>

    /** Update notification preferences. */
    suspend fun updateNotificationSettings(enabled: Boolean, muteUntil: Long?): Result<Unit>
}