package com.chatsphere.presentation.chats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatsphere.core.common.UiEvent
import com.chatsphere.core.common.UiState
import com.chatsphere.domain.model.Conversation
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.TypingState
import com.chatsphere.domain.usecase.ObserveConversationsUseCase
import com.chatsphere.domain.usecase.ObserveMessagesUseCase
import com.chatsphere.domain.usecase.SendMessageUseCase
import com.chatsphere.domain.usecase.SetTypingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(val conversations: List<Conversation> = emptyList()) : UiState

@HiltViewModel
class ChatListViewModel @Inject constructor(
    observeConversationsUseCase: ObserveConversationsUseCase
) : ViewModel() {
    val state: StateFlow<ChatListUiState> = observeConversationsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        .let { conversations ->
            MutableStateFlow(ChatListUiState()).also { state ->
                viewModelScope.launch {
                    conversations.collect { state.value = ChatListUiState(it) }
                }
            }
        }
}

data class ChatUiState(
    val conversationId: String = "",
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val typingUser: String? = null
) : UiState

sealed interface ChatEvent : UiEvent {
    data class InputChanged(val value: String) : ChatEvent
    data object SendClicked : ChatEvent
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val setTypingUseCase: SetTypingUseCase,
    private val chatRepository: com.chatsphere.domain.repository.ChatRepository
) : ViewModel() {
    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val _state = MutableStateFlow(ChatUiState(conversationId = conversationId))
    val state = _state.asStateFlow()
    private var typingJob: Job? = null

    init {
        viewModelScope.launch {
            observeMessagesUseCase(conversationId).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            chatRepository.observeTyping(conversationId).collect { state: TypingState? ->
                _state.update { it.copy(typingUser = state?.displayName) }
            }
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InputChanged -> {
                _state.update { it.copy(input = event.value) }
                debounceTyping()
            }
            ChatEvent.SendClicked -> send()
        }
    }

    private fun send() = viewModelScope.launch {
        val body = state.value.input
        if (body.isBlank()) return@launch
        _state.update { it.copy(input = "") }
        setTypingUseCase(conversationId, false)
        sendMessageUseCase(conversationId, body)
    }

    private fun debounceTyping() {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            setTypingUseCase(conversationId, true)
            delay(1_500)
            setTypingUseCase(conversationId, false)
        }
    }
}
