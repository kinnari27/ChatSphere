package com.chatsphere.data.repository

import com.chatsphere.core.common.AppResult
import com.chatsphere.core.network.TokenStore
import com.chatsphere.data.local.SessionLocalDataSource
import com.chatsphere.data.mapper.toDomain
import com.chatsphere.data.remote.ChatSphereApi
import com.chatsphere.data.remote.LoginRequest
import com.chatsphere.data.remote.RegisterRequest
import com.chatsphere.domain.model.AuthSession
import com.chatsphere.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ChatSphereApi,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val tokenStore: TokenStore
) : AuthRepository {
    override val session: Flow<AuthSession?> = sessionLocalDataSource.session

    init {
        tokenStore.update(sessionLocalDataSource.session.value?.accessToken)
    }

    override suspend fun login(email: String, password: String): AppResult<AuthSession> = runCatching {
        api.login(LoginRequest(email, password)).toDomain()
    }.fold({ persistSession(it) }, { AppResult.Error(it) })

    override suspend fun register(name: String, email: String, password: String): AppResult<AuthSession> = runCatching {
        api.register(RegisterRequest(name, email, password)).toDomain()
    }.fold({ persistSession(it) }, { AppResult.Error(it) })

    override suspend fun logout() {
        sessionLocalDataSource.clear()
        tokenStore.update(null)
    }

    private suspend fun persistSession(session: AuthSession): AppResult<AuthSession> {
        sessionLocalDataSource.save(session)
        tokenStore.update(session.accessToken)
        return AppResult.Success(session)
    }
}
