package com.chatsphere.domain.usecase.group

import com.chatsphere.domain.model.Group
import com.chatsphere.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes all groups the current user is a member of.
 */
class ObserveGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    operator fun invoke(): Flow<List<Group>> = groupRepository.observeGroups()
}

/**
 * Creates a new group with validation.
 * Minimum 2 members (excluding the creator) is enforced here.
 */
class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(
        name: String,
        memberIds: List<String>,
        avatarUrl: String?
    ): Result<Group> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        if (name.length > 50) return Result.failure(IllegalArgumentException("Group name too long"))
        if (memberIds.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one member"))
        return groupRepository.createGroup(name.trim(), memberIds, avatarUrl)
    }
}

/**
 * Adds members to an existing group.
 */
class AddGroupMembersUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String, memberIds: List<String>): Result<Unit> =
        groupRepository.addMembers(groupId, memberIds)
}

/**
 * Removes a member from the group. Admin-only on the server.
 */
class RemoveGroupMemberUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String, userId: String): Result<Unit> =
        groupRepository.removeMember(groupId, userId)
}

/**
 * Allows the current user to leave a group.
 */
class LeaveGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        groupRepository.leaveGroup(groupId)
}

