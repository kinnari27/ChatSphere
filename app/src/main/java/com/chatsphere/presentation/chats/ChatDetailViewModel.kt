// ────────────────────────────────────────────────────────────────────────

package com.chatsphere.presentation.chats.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatsphere.core.signalr.SignalRManager
import com.chatsphere.domain.model.Chat
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.MessageContent
import com.chatsphere.domain.usecase.chat.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the one-to-one and group chat detail screen.
 *
 * Manages:
 * - Message stream from Room (offline-first)
 * - Chat metadata
 * - Typing indicators with debounce
 * - Message composing state
 * - Reply/edit mode
 * - Real-time presence and typing from SignalR
 */
@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markChatAsReadUseCase: MarkChatAsReadUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val reactToMessageUseCase: ReactToMessageUseCase,
    private val pinMessageUseCase: com.chatsphere.domain.usecase.chat.PinMessageUseCase,
    private val signalRManager: SignalRManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    // ── UI State ──────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects: Flow<UiEffect> = _effects.receiveAsFlow()

    // ── Typing debounce ──────────────────────────────────────────────────

    private val _typingTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        loadMessages()
        observeTypingIndicators()
        markAsRead()
        setupTypingDebounce()
    }

    // ── Message loading ──────────────────────────────────────────────────

    private fun loadMessages() {
        observeMessagesUseCase(chatId).onEach { messages ->
            _uiState.update { it.copy(messages = messages, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    // ── Typing indicators ────────────────────────────────────────────────

    private fun observeTypingIndicators() {
        signalRManager.typingEvents
            .filter { it.chatId == chatId }
            .onEach { event ->
                _uiState.update { state ->
                    if (event.isTyping) {
                        state.copy(typingUserNames = state.typingUserNames + (event.userId to event.userName))
                    } else {
                        state.copy(typingUserNames = state.typingUserNames - event.userId)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class)
    private fun setupTypingDebounce() {
        _typingTrigger
            .debounce(TYPING_DEBOUNCE_MS)
            .onEach { signalRManager.stopTyping(chatId) }
            .launchIn(viewModelScope)
    }

    // ── Actions ──────────────────────────────────────────────────────────

    fun onTextInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        if (text.isNotEmpty()) {
            signalRManager.startTyping(chatId)
            _typingTrigger.tryEmit(Unit)
        } else {
            signalRManager.stopTyping(chatId)
        }
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", replyToMessage = null) }
            sendMessageUseCase(chatId, MessageContent.Text(text))
                .onFailure { e ->
                    _effects.send(UiEffect.ShowSnackbar("Failed to send: ${e.message}", isError = true))
                }
        }
    }

    fun sendEmojiMessage(emoji: String) {
        viewModelScope.launch {
            sendMessageUseCase(chatId, MessageContent.Emoji(emoji))
        }
    }

    fun setReplyTo(message: Message) {
        _uiState.update { it.copy(replyToMessage = message) }
    }

    fun clearReply() {
        _uiState.update { it.copy(replyToMessage = null) }
    }

    fun setEditMode(message: Message) {
        _uiState.update { it.copy(editingMessage = message, inputText = (message.content as? MessageContent.Text)?.text ?: "") }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingMessage = null, inputText = "") }
    }

    fun submitEdit() {
        val editingMessage = _uiState.value.editingMessage ?: return
        val newText = _uiState.value.inputText.trim()
        if (newText.isBlank()) return

        viewModelScope.launch {
            editMessageUseCase(editingMessage.id, newText)
                .onSuccess { _uiState.update { it.copy(editingMessage = null, inputText = "") } }
                .onFailure { e -> _effects.send(UiEffect.ShowSnackbar(e.message ?: "Edit failed", isError = true)) }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(messageId)
                .onFailure { e -> _effects.send(UiEffect.ShowSnackbar("Delete failed", isError = true)) }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            reactToMessageUseCase(messageId, emoji)
        }
    }

    fun loadMoreMessages() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreMessages) return
        _uiState.update { it.copy(isLoadingMore = true) }
        // Pagination logic — extend pageSize or use offset
        viewModelScope.launch {
            val newPageSize = _uiState.value.pageSize + PAGE_SIZE_INCREMENT
            _uiState.update { it.copy(pageSize = newPageSize, isLoadingMore = false) }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            markChatAsReadUseCase(chatId)
        }
    }

    companion object {
        private const val TYPING_DEBOUNCE_MS = 1500L
        private const val PAGE_SIZE_INCREMENT = 30
    }
}

// ── UI State data class ───────────────────────────────────────────────

data class ChatDetailUiState(
    val messages: List<Message> = emptyList(),
    val chat: Chat? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val pageSize: Int = 30,
    val inputText: String = "",
    val typingUserNames: Map<String, String> = emptyMap(),
    val replyToMessage: Message? = null,
    val editingMessage: Message? = null,
    val isRecording: Boolean = false
) {
    /** Human-readable typing indicator string: "John is typing..." */
    val typingText: String?
        get() = when (typingUserNames.size) {
            0 -> null
            1 -> "${typingUserNames.values.first()} is typing…"
            2 -> "${typingUserNames.values.joinToString(" and ")} are typing…"
            else -> "Several people are typing…"
        }
}
