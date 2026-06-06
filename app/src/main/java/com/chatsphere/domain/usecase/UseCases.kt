package com.chatsphere.domain.usecase

import com.chatsphere.domain.repository.AuthRepository
import com.chatsphere.domain.repository.ChatRepository
import com.chatsphere.domain.repository.GroupRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repository.login(email.trim(), password)
}

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(name: String, email: String, password: String) =
        repository.register(name.trim(), email.trim(), password)
}

class ObserveConversationsUseCase @Inject constructor(private val repository: ChatRepository) {
    operator fun invoke() = repository.observeConversations()
}

class ObserveMessagesUseCase @Inject constructor(private val repository: ChatRepository) {
    operator fun invoke(conversationId: String) = repository.observeMessages(conversationId)
}

class SendMessageUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(conversationId: String, body: String, replyToMessageId: String? = null) =
        repository.sendMessage(conversationId, body.trim(), replyToMessageId)
}

class SetTypingUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(conversationId: String, isTyping: Boolean) =
        repository.setTyping(conversationId, isTyping)
}

class CreateGroupUseCase @Inject constructor(private val repository: GroupRepository) {
    suspend operator fun invoke(name: String, memberIds: List<String>) =
        repository.createGroup(name.trim(), memberIds.distinct())
}
