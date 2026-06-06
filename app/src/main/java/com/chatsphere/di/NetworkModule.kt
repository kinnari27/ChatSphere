package com.chatsphere.di

import android.content.Context
import androidx.room.Room
import com.chatsphere.BuildConfig
import com.chatsphere.core.database.ChatSphereDatabase
import com.chatsphere.core.network.AuthInterceptor
import com.chatsphere.core.signalr.DefaultSignalRManager
import com.chatsphere.core.signalr.SignalRManager
import com.chatsphere.data.remote.ChatSphereApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient, json: Json): ChatSphereApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ChatSphereApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChatSphereDatabase =
        Room.databaseBuilder(context, ChatSphereDatabase::class.java, "chatsphere.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideChatDao(database: ChatSphereDatabase) = database.chatDao()
    @Provides fun provideUserDao(database: ChatSphereDatabase) = database.userDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SignalRModule {
    @Binds
    @Singleton
    abstract fun bindSignalRManager(impl: DefaultSignalRManager): SignalRManager
}
