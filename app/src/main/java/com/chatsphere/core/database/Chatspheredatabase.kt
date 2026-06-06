package com.chatsphere.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chatsphere.data.local.dao.*
import com.chatsphere.data.local.entity.*

/**
 * ChatSphere Room database.
 *
 * Version history:
 *  - v1: Initial schema with users, chats, messages, groups
 */
@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        ChatParticipantEntity::class,
        MessageEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        AuthSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ChatSphereDatabase : RoomDatabase() {

    abstract fun authSessionDao(): AuthSessionDao
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao

    companion object {
        const val DATABASE_NAME = "chatsphere.db"
    }
}