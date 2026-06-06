package com.chatsphere.di

import android.content.Context
import androidx.room.Room
import com.chatsphere.BuildConfig
import com.chatsphere.core.database.ChatSphereDatabase
import com.chatsphere.core.network.*
import com.chatsphere.core.notification.NotificationHelper
import com.chatsphere.core.signalr.TokenProvider
import com.chatsphere.data.local.dao.*
import com.chatsphere.data.remote.api.*
import com.chatsphere.data.repository.*
import com.chatsphere.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

// ─────────────────────────── Database Module ───────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChatSphereDatabase =
        Room.databaseBuilder(context, ChatSphereDatabase::class.java, ChatSphereDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAuthSessionDao(db: ChatSphereDatabase) = db.authSessionDao()
    @Provides fun provideUserDao(db: ChatSphereDatabase) = db.userDao()
    @Provides fun provideChatDao(db: ChatSphereDatabase) = db.chatDao()
    @Provides fun provideMessageDao(db: ChatSphereDatabase) = db.messageDao()
    @Provides fun provideGroupDao(db: ChatSphereDatabase) = db.groupDao()
}

// ─────────────────────────── Network Module ───────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context,
        authSessionDao: AuthSessionDao
    ): TokenStorage = DataStoreTokenStorage(context, authSessionDao)

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStorage: TokenStorage): AuthInterceptor =
        AuthInterceptor(tokenStorage)

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        buildOkHttpClient(authInterceptor)

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(okHttpClient, BuildConfig.BASE_URL)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideGroupApi(retrofit: Retrofit): GroupApi =
        retrofit.create(GroupApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideTokenProvider(tokenStorage: TokenStorage): TokenProvider =
        object : TokenProvider {
            override suspend fun getAccessToken() = tokenStorage.getAccessToken()
        }
}

// ─────────────────────────── Repository Module ───────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// ─────────────────────────── App Module ───────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)

    @Provides
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context
}