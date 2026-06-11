package com.chatsphere.data.repository

import android.net.Uri
import com.chatsphere.core.common.AppResult
import com.chatsphere.core.database.ChatDao
import com.chatsphere.data.local.SessionLocalDataSource
import com.chatsphere.data.mapper.toDomain
import com.chatsphere.domain.model.ConnectionState
import com.chatsphere.domain.model.Conversation
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.MessageStatus
import com.chatsphere.domain.model.MessageType
import com.chatsphere.domain.model.TypingState
import com.chatsphere.domain.repository.ChatRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseChatRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage,
    private val chatDao: ChatDao,
    private val sessionDataSource: SessionLocalDataSource
) : ChatRepository {

    private val messagesRef = database.getReference("messages")
    private val typingRef = database.getReference("typing")

    override fun observeConversations(): Flow<List<Conversation>> = chatDao.observeConversations()
        .map { entities -> entities.map { it.toDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = messagesRef.child(conversationId).orderByChild("createdAtEpochMillis")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { it.getValue(MessageFirebaseModel::class.java)?.toDomain() }
                    trySend(messages)
                    // Also update local cache
                    // chatDao.upsertMessages(messages.map { it.toEntity() })
                }
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            })
        awaitClose { messagesRef.child(conversationId).removeEventListener(listener) }
    }

    override fun observeTyping(conversationId: String): Flow<TypingState?> = callbackFlow {
        val listener = typingRef.child(conversationId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Simplified typing logic
                val state = snapshot.children.firstOrNull()?.getValue(TypingStateFirebaseModel::class.java)?.toDomain()
                trySend(state)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        awaitClose { typingRef.child(conversationId).removeEventListener(listener) }
    }

    override fun observeConnectionState(): Flow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)

    override suspend fun sendMessage(conversationId: String, body: String, replyToMessageId: String?): AppResult<Message> {
        val currentUser = sessionDataSource.session.value?.user ?: return AppResult.Error(Exception("Not authenticated"))
        
        val message = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            body = body,
            type = MessageType.Text,
            status = MessageStatus.Sent,
            createdAt = Instant.now(),
            replyToMessageId = replyToMessageId
        )

        return try {
            messagesRef.child(conversationId).child(message.id).setValue(message.toFirebaseModel()).await()
            AppResult.Success(message)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    override suspend fun sendMediaMessage(
        conversationId: String,
        fileUri: String,
        type: MessageType,
        replyToMessageId: String?
    ): AppResult<Message> {
        val currentUser = sessionDataSource.session.value?.user ?: return AppResult.Error(Exception("Not authenticated"))
        
        return try {
            val fileName = "${UUID.randomUUID()}_${System.currentTimeMillis()}"
            val storageRef = storage.getReference("chats/$conversationId/$fileName")
            
            val uploadTask = storageRef.putFile(Uri.parse(fileUri)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val message = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = currentUser.id,
                senderName = currentUser.displayName,
                body = downloadUrl,
                type = type,
                status = MessageStatus.Sent,
                createdAt = Instant.now(),
                replyToMessageId = replyToMessageId
            )

            messagesRef.child(conversationId).child(message.id).setValue(message.toFirebaseModel()).await()
            AppResult.Success(message)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }

    override suspend fun markAsRead(conversationId: String, messageId: String) {
        messagesRef.child(conversationId).child(messageId).child("status").setValue(MessageStatus.Read.name)
    }

    override suspend fun setTyping(conversationId: String, isTyping: Boolean) {
        val currentUser = sessionDataSource.session.value?.user ?: return
        if (isTyping) {
            typingRef.child(conversationId).child(currentUser.id).setValue(
                TypingStateFirebaseModel(conversationId, currentUser.id, currentUser.displayName, true)
            )
        } else {
            typingRef.child(conversationId).child(currentUser.id).removeValue()
        }
    }

    override suspend fun searchMessages(query: String): List<Message> = emptyList() // Implement if needed

    override suspend fun syncPendingMessages() {}
}

// Helper models for Firebase serialization
data class MessageFirebaseModel(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val body: String = "",
    val type: String = "Text",
    val status: String = "Sent",
    val createdAtEpochMillis: Long = 0,
    val replyToMessageId: String? = null
) {
    fun toDomain() = Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        body = body,
        type = MessageType.valueOf(type),
        status = MessageStatus.valueOf(status),
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        replyToMessageId = replyToMessageId
    )
}

data class TypingStateFirebaseModel(
    val conversationId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val isTyping: Boolean = false
) {
    fun toDomain() = TypingState(conversationId, userId, displayName, isTyping)
}

fun Message.toFirebaseModel() = MessageFirebaseModel(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    body = body,
    type = type.name,
    status = status.name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    replyToMessageId = replyToMessageId
)
