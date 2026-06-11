package com.chatsphere.data.repository

import com.chatsphere.core.common.AppResult
import com.chatsphere.core.network.TokenStore
import com.chatsphere.data.local.SessionLocalDataSource
import com.chatsphere.data.mapper.toDomain
import com.chatsphere.data.remote.ChatSphereApi
import com.chatsphere.data.remote.GoogleLoginRequest
import com.chatsphere.data.remote.LoginRequest
import com.chatsphere.data.remote.RegisterRequest
import com.chatsphere.domain.model.AuthSession
import com.chatsphere.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ChatSphereApi,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val tokenStore: TokenStore,
    private val firebaseAuth: FirebaseAuth
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

    override suspend fun loginWithGoogle(idToken: String): AppResult<AuthSession> = runCatching {
        // 1. Authenticate with Firebase
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw Exception("Firebase user is null")
        
        // 2. Exchange Firebase token for app session (optional, depends on backend)
        api.loginWithGoogle(GoogleLoginRequest(idToken)).toDomain()
    }.fold(
        onSuccess = { persistSession(it) },
        onFailure = { AppResult.Error(it) }
    )

    override suspend fun logout() {
        firebaseAuth.signOut()
        sessionLocalDataSource.clear()
        tokenStore.update(null)
    }

    private suspend fun persistSession(session: AuthSession): AppResult<AuthSession> {
        sessionLocalDataSource.save(session)
        tokenStore.update(session.accessToken)
        return AppResult.Success(session)
    }
}
