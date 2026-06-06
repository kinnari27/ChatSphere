package com.chatsphere.core.signalr

import com.chatsphere.BuildConfig
import com.chatsphere.domain.model.ConnectionState
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.MessageStatus
import com.chatsphere.domain.model.TypingState
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSignalRManager @Inject constructor() : SignalRManager {
    private val hubConnection: HubConnection = HubConnectionBuilder
        .create(BuildConfig.SIGNALR_HUB_URL)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userRequestedDisconnect = AtomicBoolean(false)

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    private val _typingEvents = MutableSharedFlow<TypingState>(extraBufferCapacity = 64)

    override val connectionState = _connectionState.asStateFlow()
    override val incomingMessages = _incomingMessages.asSharedFlow()
    override val typingEvents = _typingEvents.asSharedFlow()

    init {
        hubConnection.onClosed {
            _connectionState.value = ConnectionState.Disconnected
            if (!userRequestedDisconnect.get()) {
                scope.launch { connect() }
            }
        }
        registerEvents()
    }

    override suspend fun connect() {
        if (_connectionState.value == ConnectionState.Connected) return
        userRequestedDisconnect.set(false)
        _connectionState.value = ConnectionState.Connecting
        retryWithBackoff {
            hubConnection.start().await()
            _connectionState.value = ConnectionState.Connected
        }
    }

    override suspend fun disconnect() {
        userRequestedDisconnect.set(true)
        hubConnection.stop().await()
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendMessage(conversationId: String, body: String, replyToMessageId: String?) {
        hubConnection.send("SendMessage", conversationId, body, replyToMessageId)
    }

    override suspend fun startTyping(conversationId: String) {
        hubConnection.send("UserTyping", conversationId)
    }

    override suspend fun stopTyping(conversationId: String) {
        hubConnection.send("UserStoppedTyping", conversationId)
    }

    private fun registerEvents() {
        hubConnection.on(
            "ReceiveMessage",
            { id: String, conversationId: String, senderId: String, senderName: String, body: String, epochMillis: Long ->
                _incomingMessages.tryEmit(
                    Message(
                        id = id,
                        conversationId = conversationId,
                        senderId = senderId,
                        senderName = senderName,
                        body = body,
                        status = MessageStatus.Delivered,
                        createdAt = Instant.ofEpochMilli(epochMillis)
                    )
                )
            },
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            Long::class.java
        )
        hubConnection.on(
            "UserTyping",
            { conversationId: String, userId: String, displayName: String ->
                _typingEvents.tryEmit(TypingState(conversationId, userId, displayName, true))
            },
            String::class.java,
            String::class.java,
            String::class.java
        )
        hubConnection.on(
            "UserStoppedTyping",
            { conversationId: String, userId: String, displayName: String ->
                _typingEvents.tryEmit(TypingState(conversationId, userId, displayName, false))
            },
            String::class.java,
            String::class.java,
            String::class.java
        )
    }

    private suspend fun retryWithBackoff(block: suspend () -> Unit) {
        var delayMillis = 1_000L
        repeat(5) {
            runCatching { block() }
                .onSuccess { return }
            _connectionState.value = ConnectionState.Reconnecting
            delay(delayMillis)
            delayMillis = (delayMillis * 2).coerceAtMost(30_000)
        }
        _connectionState.value = ConnectionState.Disconnected
    }
}
