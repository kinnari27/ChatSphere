package com.chatsphere.domain.usecase.chat

import com.chatsphere.domain.model.Chat
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.MessageContent
import com.chatsphere.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a live stream of all conversations, sorted by last activity.
 */
class ObserveChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<Chat>> = chatRepository.observeChats()
}

/**
 * Observes messages for a specific chat with pagination support.
 */
class ObserveMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(chatId: String, pageSize: Int = 30): Flow<List<Message>> =
        chatRepository.observeMessages(chatId, pageSize)
}

/**
 * Sends a message with offline queuing.
 * The message is written to the local Room database immediately (Pending status),
 * then the repository attempts to sync it with the server.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, content: MessageContent): Result<Message> {
        if (chatId.isBlank()) return Result.failure(IllegalArgumentException("Chat ID is required"))

        // Validate text content is not empty
        if (content is MessageContent.Text && content.text.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }

        return chatRepository.sendMessage(chatId, content)
    }
}

/**
 * Marks all messages in a chat as read and resets the unread badge.
 */
class MarkChatAsReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String): Result<Unit> =
        chatRepository.markChatAsRead(chatId)
}

/**
 * Full-text search across all cached messages.
 */
class SearchMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(query: String): Result<List<Message>> {
        if (query.length < 2) return Result.success(emptyList())
        return chatRepository.searchMessages(query.trim())
    }
}

/**
 * Toggles the archived state of a conversation.
 */
class ArchiveChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, archive: Boolean): Result<Unit> =
        chatRepository.archiveChat(chatId, archive)
}

/**
 * Adds an emoji reaction to a message.
 */
class ReactToMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(messageId: String, emoji: String): Result<Unit> =
        chatRepository.reactToMessage(messageId, emoji)
}

/**
 * Retries sending all locally-queued (Pending/Failed) messages.
 * Called when connectivity is restored.
 */
class SyncPendingMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(): Result<Unit> = chatRepository.syncPendingMessages()
}

/**
 * Deletes a message by ID.
 */
class DeleteMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> =
        chatRepository.deleteMessage(messageId)
}

/**
 * Edits the text of an existing message.
 */
class EditMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(messageId: String, newText: String): Result<Message> {
        if (newText.isBlank()) return Result.failure(IllegalArgumentException("Edited message cannot be empty"))
        return chatRepository.editMessage(messageId, newText)
    }
}