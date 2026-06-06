package com.chatsphere.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.chatsphere.domain.model.ConversationType
import com.chatsphere.domain.model.MessageStatus
import com.chatsphere.domain.model.MessageType

@Database(
    entities = [UserEntity::class, ConversationEntity::class, MessageEntity::class, PendingMessageEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(ChatSphereConverters::class)
abstract class ChatSphereDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao
}

class ChatSphereConverters {
    @TypeConverter fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
    @TypeConverter fun fromMessageStatus(value: MessageStatus): String = value.name
    @TypeConverter fun toMessageType(value: String): MessageType = MessageType.valueOf(value)
    @TypeConverter fun fromMessageType(value: MessageType): String = value.name
    @TypeConverter fun toConversationType(value: String): ConversationType = ConversationType.valueOf(value)
    @TypeConverter fun fromConversationType(value: ConversationType): String = value.name
}
