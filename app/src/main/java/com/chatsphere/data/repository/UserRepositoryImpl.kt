package com.chatsphere.data.repository

import com.chatsphere.core.database.UserDao
import com.chatsphere.data.mapper.toDomain
import com.chatsphere.domain.repository.UserRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(private val userDao: UserDao) : UserRepository {
    override fun observeUsers() = userDao.observeUsers().map { users -> users.map { it.toDomain() } }
    override suspend fun blockUser(userId: String) = userDao.blockUser(userId)
    override suspend fun updateProfile(displayName: String, avatarUrl: String?) = Unit
}
