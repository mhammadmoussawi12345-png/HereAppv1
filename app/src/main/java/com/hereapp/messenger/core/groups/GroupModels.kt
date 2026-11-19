package com.hereapp.messenger.core.groups

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class GroupRole {
    OWNER,
    ADMIN,
    MODERATOR,
    MEMBER
}

data class GroupPermissions(
    val membersCanSendMessages: Boolean = true,
    val membersCanAddOthers: Boolean = false,
    val moderatorsCanPinMessages: Boolean = true,
    val moderatorsCanRemoveMembers: Boolean = true
)

data class GroupMember(
    val uid: String,
    val username: String,
    val displayName: String,
    val role: GroupRole = GroupRole.MEMBER
)

data class HereGroup(
    val id: String,
    val name: String,
    val description: String = "",
    val photoUri: String? = null,
    val isPublic: Boolean = false,
    val members: List<GroupMember> = emptyList(),
    val permissions: GroupPermissions = GroupPermissions(),
    val createdAtEpochMs: Long = 0L
)

interface GroupsRepository {
    fun observeMyGroups(): Flow<List<HereGroup>>
    fun observePublicGroups(query: String): Flow<List<HereGroup>>
    suspend fun createGroup(
        name: String,
        description: String,
        photoUri: String?,
        isPublic: Boolean,
        permissions: GroupPermissions
    ): Result<HereGroup>

    suspend fun joinGroup(groupId: String): Result<Unit>
    suspend fun leaveGroup(groupId: String): Result<Unit>
    suspend fun addMember(groupId: String, username: String): Result<Unit>
    suspend fun removeMember(groupId: String, memberUid: String): Result<Unit>
    suspend fun updateMemberRole(groupId: String, memberUid: String, role: GroupRole): Result<Unit>
}

class UnprovisionedGroupsRepository : GroupsRepository {
    private fun unconfigured(): Result<Nothing> =
        Result.failure(
            IllegalStateException(
                "Cloud backend (Firestore) is not provisioned. Enable Firebase to create, discover, and sync Here Groups."
            )
        )

    override fun observeMyGroups(): Flow<List<HereGroup>> = flowOf(emptyList())
    override fun observePublicGroups(query: String): Flow<List<HereGroup>> = flowOf(emptyList())

    override suspend fun createGroup(
        name: String,
        description: String,
        photoUri: String?,
        isPublic: Boolean,
        permissions: GroupPermissions
    ): Result<HereGroup> = unconfigured()

    override suspend fun joinGroup(groupId: String): Result<Unit> = unconfigured()
    override suspend fun leaveGroup(groupId: String): Result<Unit> = unconfigured()
    override suspend fun addMember(groupId: String, username: String): Result<Unit> = unconfigured()
    override suspend fun removeMember(groupId: String, memberUid: String): Result<Unit> = unconfigured()
    override suspend fun updateMemberRole(groupId: String, memberUid: String, role: GroupRole): Result<Unit> = unconfigured()
}
