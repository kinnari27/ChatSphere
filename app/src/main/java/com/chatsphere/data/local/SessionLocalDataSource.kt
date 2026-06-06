package com.chatsphere.data.local

import android.content.Context
import com.chatsphere.domain.model.AuthSession
import com.chatsphere.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLocalDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("chat_sphere_session", Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(readSession())
    val session = _session.asStateFlow()

    suspend fun save(session: AuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .putString(KEY_AVATAR_URL, session.user.avatarUrl)
            .apply()
        _session.value = session
    }

    suspend fun clear() {
        preferences.edit().clear().apply()
        _session.value = null
    }

    private fun readSession(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val displayName = preferences.getString(KEY_DISPLAY_NAME, null).orEmpty()
        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = User(
                id = userId,
                displayName = displayName.ifBlank { "You" },
                avatarUrl = preferences.getString(KEY_AVATAR_URL, null)
            )
        )
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_AVATAR_URL = "avatar_url"
    }
}
