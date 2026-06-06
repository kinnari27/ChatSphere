package com.chatsphere.core.signalr

import com.chatsphere.domain.model.ConnectionState
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.TypingState
import kotlinx.coroutines.flow.Flow

/**
 * Owns the lifecycle of the SignalR hub and exposes hot streams for realtime chat events.
 *
 * Keeping this contract in core allows repositories to depend on a stable boundary while the
 * concrete implementation can evolve with authentication, heartbeat, and reconnection policy.
 */
interface SignalRManager {
    val connectionState: Flow<ConnectionState>
    val incomingMessages: Flow<Message>
    val typingEvents: Flow<TypingState>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun sendMessage(conversationId: String, body: String, replyToMessageId: String?)
    suspend fun startTyping(conversationId: String)
    suspend fun stopTyping(conversationId: String)
}
