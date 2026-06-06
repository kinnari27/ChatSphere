package com.chatsphere.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.chatsphere.R
import com.chatsphere.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service.
 *
 * Handles:
 * - [onMessageReceived]: foreground and background push notification routing
 * - [onNewToken]: registers the device FCM token with the ChatSphere backend
 *
 * Notification payload expected:
 * ```json
 * {
 *   "type": "chat",
 *   "chatId": "abc123",
 *   "sender": "John",
 *   "message": "Hello there!",
 *   "avatarUrl": "https://..."
 * }
 * ```
 */
@AndroidEntryPoint
class ChatSphereFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isEmpty()) {
            // Handle notification-type messages (auto-displayed by OS in background)
            remoteMessage.notification?.let { notification ->
                notificationHelper.showSimpleNotification(
                    title = notification.title ?: "New message",
                    body = notification.body ?: "",
                    chatId = null
                )
            }
            return
        }

        val type = data["type"] ?: return
        val chatId = data["chatId"] ?: return
        val sender = data["sender"] ?: "Someone"
        val message = data["message"] ?: ""
        val avatarUrl = data["avatarUrl"]

        when (type) {
            "chat", "group_message" -> {
                notificationHelper.showChatNotification(
                    chatId = chatId,
                    senderName = sender,
                    message = message,
                    avatarUrl = avatarUrl
                )
            }
            "mention" -> {
                notificationHelper.showMentionNotification(
                    chatId = chatId,
                    senderName = sender,
                    message = message
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed")
        serviceScope.launch {
            authRepository.updateFcmToken(token)
                .onFailure { e -> Timber.e(e, "Failed to update FCM token") }
        }
    }
}

// ─────────────────────────────────────────────────────────────

/**
 * Creates and posts Android notifications with proper channels and deep-link intents.
 */
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    init {
        createNotificationChannels()
    }

    /** Displays a notification that deep-links to the specified chat. */
    fun showChatNotification(
        chatId: String,
        senderName: String,
        message: String,
        avatarUrl: String?
    ) {
        val intent = buildDeepLinkIntent(chatId)
        val pendingIntent = PendingIntent.getActivity(
            context, chatId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        notificationManager.notify(chatId.hashCode(), notification)
    }

    fun showMentionNotification(chatId: String, senderName: String, message: String) {
        val intent = buildDeepLinkIntent(chatId)
        val pendingIntent = PendingIntent.getActivity(
            context, chatId.hashCode() + 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MENTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("@$senderName mentioned you")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(chatId.hashCode() + 1, notification)
    }

    fun showSimpleNotification(title: String, body: String, chatId: String?) {
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /** Dismiss all notifications for a specific chat (called when user opens it). */
    fun dismissNotificationsForChat(chatId: String) {
        notificationManager.cancel(chatId.hashCode())
    }

    private fun buildDeepLinkIntent(chatId: String): Intent =
        Intent(context, Class.forName("com.chatsphere.presentation.MainActivity")).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_CHAT_ID, chatId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val messageChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New chat messages"
            enableVibration(true)
        }

        val mentionChannel = NotificationChannel(
            CHANNEL_MENTIONS,
            "Mentions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "When someone mentions you"
            enableVibration(true)
        }

        notificationManager.createNotificationChannels(listOf(messageChannel, mentionChannel))
    }

    companion object {
        const val CHANNEL_MESSAGES = "chatsphere_messages"
        const val CHANNEL_MENTIONS = "chatsphere_mentions"
        const val EXTRA_CHAT_ID = "chat_id"
    }
}