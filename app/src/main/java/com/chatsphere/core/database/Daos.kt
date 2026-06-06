package com.chatsphere.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY unreadCount DESC, title ASC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAtEpochMillis ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE body LIKE '%' || :query || '%' ORDER BY createdAtEpochMillis DESC")
    suspend fun searchMessages(query: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(entity: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(entities: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(entity: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: com.chatsphere.domain.model.MessageStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueuePending(entity: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages ORDER BY createdAtEpochMillis ASC")
    suspend fun pendingMessages(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE localId = :localId")
    suspend fun removePending(localId: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isOnline = :isOnline, lastSeenEpochMillis = :lastSeen WHERE id = :userId")
    suspend fun updatePresence(userId: String, isOnline: Boolean, lastSeen: Long?)

    @Query("UPDATE users SET isBlocked = 1 WHERE id = :userId")
    suspend fun blockUser(userId: String)
}
