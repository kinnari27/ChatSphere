// ── Placeholder use case for compilation ─────────────────────────────

package com.chatsphere.domain.usecase.chat

import com.chatsphere.domain.repository.ChatRepository
import javax.inject.Inject

class PinMessageUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(messageId: String) = chatRepository.pinMessage(messageId)
}