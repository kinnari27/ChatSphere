package com.chatsphere.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chatsphere.domain.model.MessageStatus
import com.chatsphere.domain.model.UserPresence

// ────────────────────── UserAvatar ──────────────────────────────────

/**
 * Circular avatar with initials fallback and optional online badge.
 *
 * @param avatarUrl  Remote image URL (Coil loads it). If null, initials are shown.
 * @param displayName  Used to derive initials when no image is available.
 * @param size  Diameter of the avatar.
 * @param presence  When non-null, renders a small online/offline badge.
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    presence: UserPresence? = null
) {
    Box(modifier = modifier.size(size)) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar of $displayName",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            // Initials fallback
            val initials = displayName
                .split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Online badge
        if (presence != null) {
            OnlineBadge(
                presence = presence,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

// ────────────────────── OnlineBadge ──────────────────────────────────

/**
 * Small colored dot indicating user presence state.
 */
@Composable
fun OnlineBadge(
    presence: UserPresence,
    modifier: Modifier = Modifier
) {
    val color = when (presence) {
        UserPresence.Online -> Color(0xFF4CAF50)
        UserPresence.Away -> Color(0xFFFFC107)
        UserPresence.Offline -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(1.5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ────────────────────── ChatBubble ──────────────────────────────────

/**
 * Message bubble supporting sent/received alignment, reply previews,
 * delivery status icons, and emoji messages (no bubble background).
 */
@Composable
fun ChatBubble(
    message: com.chatsphere.domain.model.Message,
    isSentByCurrentUser: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmoji = message.content is com.chatsphere.domain.model.MessageContent.Emoji

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Reply preview
        message.replyTo?.let { reply ->
            ReplyPreviewBubble(
                senderName = reply.senderName,
                previewText = reply.previewText,
                isSentByCurrentUser = isSentByCurrentUser
            )
        }

        if (isEmoji) {
            // Large emoji — no bubble
            val emoji = (message.content as com.chatsphere.domain.model.MessageContent.Emoji).unicode
            Text(text = emoji, fontSize = 40.sp, modifier = Modifier.padding(4.dp))
        } else {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isSentByCurrentUser) 18.dp else 4.dp,
                    topEnd = if (isSentByCurrentUser) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = if (isSentByCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clickable(onClick = {}) // long press via combinedClickable in real impl
            ) {
                Column(modifier = Modifier.padding(10.dp, 8.dp)) {
                    // Sender name for group chats
                    if (!isSentByCurrentUser) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    when (val content = message.content) {
                        is com.chatsphere.domain.model.MessageContent.Text -> {
                            Text(
                                text = content.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSentByCurrentUser)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is com.chatsphere.domain.model.MessageContent.Image -> {
                            AsyncImage(
                                model = content.url,
                                contentDescription = "Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        else -> {}
                    }

                    // Timestamp + status row
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.isEdited) {
                            Text("edited", style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = message.createdAt.toFormattedTime(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isSentByCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.outline
                        )
                        if (isSentByCurrentUser) {
                            MessageStatusIcon(status = message.status)
                        }
                    }
                }
            }
        }

        // Reactions
        if (message.reactions.isNotEmpty()) {
            ReactionsRow(reactions = message.reactions)
        }
    }
}

// ────────────────────── TypingIndicator ──────────────────────────────

/**
 * Animated three-dot typing indicator with pulsing animation.
 *
 * @param typingText  e.g. "John is typing…" — shown above the dots.
 */
@Composable
fun TypingIndicator(typingText: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = typingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ────────────────────── ChatToolbar ──────────────────────────────────

/**
 * Top app bar for the chat detail screen showing avatar, name, and presence.
 */
@Composable
fun ChatToolbar(
    title: String,
    subtitle: String?,
    avatarUrl: String?,
    onNavigateBack: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            UserAvatar(
                avatarUrl = avatarUrl,
                displayName = title,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = {}) {
                Icon(Icons.Default.Call, contentDescription = "Call")
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        }
    }
}

// ────────────────────── MessageStatusIcon ─────────────────────────────

@Composable
fun MessageStatusIcon(status: MessageStatus, modifier: Modifier = Modifier) {
    val (icon, color) = when (status) {
        MessageStatus.Pending -> Icons.Default.Schedule to MaterialTheme.colorScheme.onPrimary.copy(0.5f)
        MessageStatus.Sent -> Icons.Default.Done to MaterialTheme.colorScheme.onPrimary.copy(0.7f)
        MessageStatus.Delivered -> Icons.Default.DoneAll to MaterialTheme.colorScheme.onPrimary.copy(0.7f)
        MessageStatus.Read -> Icons.Default.DoneAll to Color(0xFF4FC3F7)
        MessageStatus.Failed -> Icons.Default.Error to MaterialTheme.colorScheme.error
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = modifier.size(14.dp),
        tint = color
    )
}

// ────────────────────── EmptyState ────────────────────────────────────

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ChatBubbleOutline,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

// ────────────────────── DaySeparator ──────────────────────────────────

@Composable
fun DaySeparator(label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

// ────────────────────── Helpers ──────────────────────────────────────

@Composable
private fun ReplyPreviewBubble(senderName: String, previewText: String, isSentByCurrentUser: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSentByCurrentUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.widthIn(max = 260.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(senderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(previewText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
private fun ReactionsRow(reactions: List<com.chatsphere.domain.model.MessageReaction>) {
    val grouped = reactions.groupBy { it.emoji }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
        grouped.forEach { (emoji, list) ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 1.dp) {
                Text("$emoji ${list.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun java.time.Instant.toFormattedTime(): String {
    val ldt = atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return String.format("%02d:%02d", ldt.hour, ldt.minute)
}