package com.chatsphere.presentation.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chatsphere.domain.model.User
import com.chatsphere.presentation.components.ChatBubble
import com.chatsphere.presentation.components.ChatToolbar
import com.chatsphere.presentation.components.EmptyState
import com.chatsphere.presentation.components.TypingIndicator
import com.chatsphere.presentation.components.UserAvatar

@Composable
fun ChatListScreen(
    onChatSelected: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    if (state.conversations.isEmpty()) {
        EmptyState("No conversations yet", "Messages and groups will appear here.", Modifier.fillMaxSize())
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.conversations, key = { it.id }) { conversation ->
                ListItem(
                    headlineContent = { Text(conversation.title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(conversation.lastMessage?.body ?: "Tap to open chat", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingContent = { UserAvatar(User(id = conversation.id, displayName = conversation.title, avatarUrl = conversation.avatarUrl)) },
                    trailingContent = {
                        if (conversation.unreadCount > 0) Text(conversation.unreadCount.toString(), color = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { onChatSelected(conversation.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = { ChatToolbar(title = "Conversation", subtitle = state.typingUser?.let { "$it typing" }) },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = { viewModel.onEvent(ChatEvent.InputChanged(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") }
                )
                IconButton(onClick = { viewModel.onEvent(ChatEvent.SendClicked) }) {
                    Icon(Icons.Outlined.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                reverseLayout = false
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        isMine = message.senderId == "me",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            TypingIndicator(state.typingUser)
        }
    }
}
