package com.chatsphere.domain.repository

import com.chatsphere.core.common.AppResult
import com.chatsphere.domain.model.AuthSession
import com.chatsphere.domain.model.ConnectionState
import com.chatsphere.domain.model.Conversation
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.MessageType
import com.chatsphere.domain.model.TypingState
import com.chatsphere.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Coordinates authentication APIs, persisted JWT session state, and logout cleanup. */
interface AuthRepository {
    val session: Flow<AuthSession?>
    suspend fun login(email: String, password: String): AppResult<AuthSession>
    suspend fun register(name: String, email: String, password: String): AppResult<AuthSession>
    suspend fun loginWithGoogle(idToken: String): AppResult<AuthSession>
    suspend fun logout()
}

/** Offline-first source of truth for conversations, messages, typing, presence, and sync. */
interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    fun observeTyping(conversationId: String): Flow<TypingState?>
    fun observeConnectionState(): Flow<ConnectionState>
    suspend fun sendMessage(conversationId: String, body: String, replyToMessageId: String? = null): AppResult<Message>
    suspend fun sendMediaMessage(conversationId: String, fileUri: String, type: MessageType, replyToMessageId: String? = null): AppResult<Message>
    suspend fun markAsRead(conversationId: String, messageId: String)
    suspend fun setTyping(conversationId: String, isTyping: Boolean)
    suspend fun searchMessages(query: String): List<Message>
    suspend fun syncPendingMessages()
}

/** Handles group creation and membership management. */
interface GroupRepository {
    suspend fun createGroup(name: String, memberIds: List<String>): AppResult<Conversation>
    suspend fun addMembers(groupId: String, memberIds: List<String>)
    suspend fun removeMember(groupId: String, memberId: String)
}

/** Exposes user directory, presence, blocking, and profile operations. */
interface UserRepository {
    fun observeUsers(): Flow<List<User>>
    suspend fun blockUser(userId: String)
    suspend fun updateProfile(displayName: String, avatarUrl: String?)
}
