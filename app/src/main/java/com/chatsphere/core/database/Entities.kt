package com.chatsphere.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatsphere.domain.model.ConversationType
import com.chatsphere.domain.model.MessageStatus
import com.chatsphere.domain.model.MessageType

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val lastSeenEpochMillis: Long?,
    val isBlocked: Boolean
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: ConversationType,
    val avatarUrl: String?,
    val unreadCount: Int,
    val isArchived: Boolean
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val type: MessageType,
    val status: MessageStatus,
    val createdAtEpochMillis: Long,
    val replyToMessageId: String?,
    val reactionSummary: String,
    val isPinned: Boolean
)

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey val localId: String,
    val conversationId: String,
    val body: String,
    val replyToMessageId: String?,
    val createdAtEpochMillis: Long
)
