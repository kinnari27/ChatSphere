package com.chatsphere.di

import com.chatsphere.data.repository.AuthRepositoryImpl
import com.chatsphere.data.repository.ChatRepositoryImpl
import com.chatsphere.data.repository.GroupRepositoryImpl
import com.chatsphere.data.repository.UserRepositoryImpl
import com.chatsphere.domain.repository.AuthRepository
import com.chatsphere.domain.repository.ChatRepository
import com.chatsphere.domain.repository.GroupRepository
import com.chatsphere.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
    @Binds @Singleton abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
