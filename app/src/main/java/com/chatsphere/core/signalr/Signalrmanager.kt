package com.chatsphere.core.signalr

import com.chatsphere.data.remote.dto.MessageDto
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the persistent SignalR WebSocket connection.
 *
 * Responsibilities:
 * - Establishes and maintains the Hub connection
 * - Registers all real-time event handlers
 * - Provides observable Flows for each event type
 * - Handles automatic reconnection with exponential backoff
 * - Tracks connection state for UI indicators
 *
 * Architecture: Singleton, injected via Hilt.
 * All events are cold flows backed by SharedFlow replay caches.
 */
@Singleton
class SignalRManager @Inject constructor(
    private val tokenProvider: TokenProvider
) {
    // ─────────────────────────── Connection State ───────────────────────────

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // ─────────────────────────── Event Flows ───────────────────────────

    private val _incomingMessages = MutableSharedFlow<MessageDto>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingMessages: SharedFlow<MessageDto> = _incomingMessages.asSharedFlow()

    private val _messageStatusUpdates = MutableSharedFlow<MessageStatusUpdate>(
        extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageStatusUpdates: SharedFlow<MessageStatusUpdate> = _messageStatusUpdates.asSharedFlow()

    private val _typingEvents = MutableSharedFlow<TypingEvent>(
        extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val typingEvents: SharedFlow<TypingEvent> = _typingEvents.asSharedFlow()

    private val _presenceEvents = MutableSharedFlow<PresenceEvent>(
        extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val presenceEvents: SharedFlow<PresenceEvent> = _presenceEvents.asSharedFlow()

    // ─────────────────────────── Hub Connection ───────────────────────────

    private var hubConnection: HubConnection? = null

    /**
     * Establishes the SignalR hub connection with JWT auth header.
     * Registers all event handlers before connecting.
     */
    suspend fun connect() {
        if (_connectionState.value == ConnectionState.Connected) return

        _connectionState.value = ConnectionState.Connecting

        try {
            val token = tokenProvider.getAccessToken()
                ?: throw IllegalStateException("No auth token available")

            val connection = HubConnectionBuilder
                .create(HUB_URL)
                .withHeader("Authorization", "Bearer $token")
                .withAutomaticReconnect(LongArray(RECONNECT_INTERVALS.size) { RECONNECT_INTERVALS[it] })
                .build()

            registerEventHandlers(connection)
            setupConnectionCallbacks(connection)

            connection.startAsync().blockingAwait(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            hubConnection = connection
            _connectionState.value = ConnectionState.Connected
            Timber.d("SignalR connected successfully")
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            Timber.e(e, "SignalR connection failed")
            throw e
        }
    }

    /** Gracefully close the connection. */
    suspend fun disconnect() {
        hubConnection?.stopAsync()?.blockingAwait()
        hubConnection = null
        _connectionState.value = ConnectionState.Disconnected
    }

    // ─────────────────────────── Event Handlers ───────────────────────────

    private fun registerEventHandlers(connection: HubConnection) {
        // Incoming message
        connection.on(EVENT_MESSAGE_RECEIVED, { message: MessageDto ->
            _incomingMessages.tryEmit(message)
        }, MessageDto::class.java)

        // Read receipt
        connection.on(EVENT_MESSAGE_READ, { chatId: String, userId: String ->
            _messageStatusUpdates.tryEmit(
                MessageStatusUpdate(chatId = chatId, userId = userId, status = "read")
            )
        }, String::class.java, String::class.java)

        // Delivered receipt
        connection.on(EVENT_MESSAGE_DELIVERED, { messageId: String ->
            _messageStatusUpdates.tryEmit(
                MessageStatusUpdate(messageId = messageId, status = "delivered")
            )
        }, String::class.java)

        // Typing started
        connection.on(EVENT_USER_TYPING, { chatId: String, userId: String, userName: String ->
            _typingEvents.tryEmit(TypingEvent(chatId, userId, userName, isTyping = true))
        }, String::class.java, String::class.java, String::class.java)

        // Typing stopped
        connection.on(EVENT_USER_STOPPED_TYPING, { chatId: String, userId: String ->
            _typingEvents.tryEmit(TypingEvent(chatId, userId, "", isTyping = false))
        }, String::class.java, String::class.java)

        // User came online
        connection.on(EVENT_USER_ONLINE, { userId: String ->
            _presenceEvents.tryEmit(PresenceEvent(userId, isOnline = true))
        }, String::class.java)

        // User went offline
        connection.on(EVENT_USER_OFFLINE, { userId: String, lastSeen: Long ->
            _presenceEvents.tryEmit(PresenceEvent(userId, isOnline = false, lastSeen = lastSeen))
        }, String::class.java, Long::class.java)
    }

    private fun setupConnectionCallbacks(connection: HubConnection) {
        connection.onClosed { error ->
            _connectionState.value = if (error != null) {
                Timber.e(error, "SignalR connection closed with error")
                ConnectionState.Error(error.message ?: "Connection lost")
            } else {
                ConnectionState.Disconnected
            }
        }

        connection.onReconnecting { error ->
            Timber.w("SignalR reconnecting: ${error?.message}")
            _connectionState.value = ConnectionState.Reconnecting
        }

        connection.onReconnected { connectionId ->
            Timber.d("SignalR reconnected: $connectionId")
            _connectionState.value = ConnectionState.Connected
        }
    }

    // ─────────────────────────── Outbound Methods ───────────────────────────

    /** Notify the hub that the current user started typing in a chat. */
    fun startTyping(chatId: String) {
        invokeOnConnection(METHOD_TYPING_START, chatId)
    }

    /** Notify the hub that the current user stopped typing. */
    fun stopTyping(chatId: String) {
        invokeOnConnection(METHOD_TYPING_STOP, chatId)
    }

    /** Acknowledge that a message was delivered to this device. */
    fun acknowledgeDelivered(messageId: String) {
        invokeOnConnection(METHOD_ACKNOWLEDGE_DELIVERED, messageId)
    }

    /** Notify the hub that all messages in a chat have been seen. */
    fun markRead(chatId: String) {
        invokeOnConnection(METHOD_MARK_READ, chatId)
    }

    private fun invokeOnConnection(method: String, vararg args: Any) {
        if (hubConnection?.connectionState == HubConnectionState.CONNECTED) {
            try {
                hubConnection?.invoke(method, *args)
            } catch (e: Exception) {
                Timber.e(e, "Failed to invoke SignalR method: $method")
            }
        } else {
            Timber.w("Cannot invoke $method — not connected (state: ${hubConnection?.connectionState})")
        }
    }

    // ─────────────────────────── Constants ───────────────────────────

    companion object {
        private const val HUB_URL = "https://api.chatsphere.com/hubs/chat"
        private const val CONNECT_TIMEOUT_SECONDS = 10L

        // Exponential backoff in ms: 0, 2s, 10s, 30s, ∞
        private val RECONNECT_INTERVALS = longArrayOf(0, 2_000, 10_000, 30_000)

        // Server → Client events
        private const val EVENT_MESSAGE_RECEIVED = "MessageReceived"
        private const val EVENT_MESSAGE_READ = "MessageRead"
        private const val EVENT_MESSAGE_DELIVERED = "MessageDelivered"
        private const val EVENT_USER_TYPING = "UserTyping"
        private const val EVENT_USER_STOPPED_TYPING = "UserStoppedTyping"
        private const val EVENT_USER_ONLINE = "UserOnline"
        private const val EVENT_USER_OFFLINE = "UserOffline"

        // Client → Server methods
        private const val METHOD_TYPING_START = "StartTyping"
        private const val METHOD_TYPING_STOP = "StopTyping"
        private const val METHOD_ACKNOWLEDGE_DELIVERED = "AcknowledgeDelivered"
        private const val METHOD_MARK_READ = "MarkRead"
    }
}

// ─────────────────────────── Value Types ───────────────────────────

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Reconnecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class MessageStatusUpdate(
    val messageId: String? = null,
    val chatId: String? = null,
    val userId: String? = null,
    val status: String
)

data class TypingEvent(
    val chatId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean
)

data class PresenceEvent(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long? = null
)

/** Provides the current JWT access token for the SignalR auth header. */
interface TokenProvider {
    suspend fun getAccessToken(): String?
}