package com.chatsphere.data.repository

import com.chatsphere.core.common.AppResult
import com.chatsphere.core.database.ChatDao
import com.chatsphere.data.mapper.toDomain
import com.chatsphere.data.mapper.toEntity
import com.chatsphere.data.remote.ChatSphereApi
import com.chatsphere.data.remote.CreateGroupRequest
import com.chatsphere.domain.model.Conversation
import com.chatsphere.domain.repository.GroupRepository
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val api: ChatSphereApi,
    private val chatDao: ChatDao
) : GroupRepository {
    override suspend fun createGroup(name: String, memberIds: List<String>): AppResult<Conversation> = runCatching {
        api.createGroup(CreateGroupRequest(name, memberIds)).toEntity()
    }.fold(
        onSuccess = {
            chatDao.upsertConversation(it)
            AppResult.Success(it.toDomain())
        },
        onFailure = { AppResult.Error(it) }
    )

    override suspend fun addMembers(groupId: String, memberIds: List<String>) = Unit
    override suspend fun removeMember(groupId: String, memberId: String) = Unit
}
