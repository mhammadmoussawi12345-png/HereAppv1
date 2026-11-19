package com.hereapp.messenger.core.security

enum class BadgeType(val label: String) {
    FOUNDER_OWNER("Founder & Owner"),
    VERIFIED("Verified"),
    MODERATOR("Moderator"),
    CUSTOM("Special")
}

data class UserBadge(
    val type: BadgeType,
    val customLabel: String? = null
) {
    val displayLabel: String get() = customLabel ?: type.label
}

data class PrivacySettings(
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    val showReadReceipts: Boolean = true
)

data class ManagedUserRecord(
    val firebaseUid: String,
    val username: String,
    val displayName: String,
    val hereAppId: String? = null,
    val isOwner: Boolean = false,
    val isVerified: Boolean = false,
    val isModerator: Boolean = false,
    val isSuspended: Boolean = false,
    val badges: List<UserBadge> = emptyList()
)

data class AdminAuditLogEntry(
    val id: String,
    val actorUid: String,
    val action: String,
    val targetIdentifier: String,
    val timestampEpochMs: Long
)

data class UserReportRecord(
    val id: String,
    val reporterUid: String,
    val targetUsername: String,
    val reason: String,
    val status: String
)

interface SecurityAdminRepository {
    suspend fun searchUserByIdOrUsername(query: String): Result<ManagedUserRecord?>
    suspend fun setUserVerified(targetUid: String, verified: Boolean): Result<Unit>
    suspend fun setUserModerator(targetUid: String, moderator: Boolean): Result<Unit>
    suspend fun setUserSuspended(targetUid: String, suspended: Boolean): Result<Unit>
    suspend fun blockUser(targetUid: String): Result<Unit>
    suspend fun reportUser(targetUid: String, reason: String): Result<Unit>
    suspend fun deleteCurrentAccount(): Result<Unit>
    suspend fun fetchAuditLogs(): Result<List<AdminAuditLogEntry>>
    suspend fun fetchReports(): Result<List<UserReportRecord>>
}

class UnprovisionedSecurityAdminRepository : SecurityAdminRepository {
    private fun unconfigured(): Result<Nothing> =
        Result.failure(
            IllegalStateException(
                "Backend security & Owner Control Center require Firebase Auth + Firestore with deployed firestore.rules."
            )
        )

    override suspend fun searchUserByIdOrUsername(query: String): Result<ManagedUserRecord?> = unconfigured()
    override suspend fun setUserVerified(targetUid: String, verified: Boolean): Result<Unit> = unconfigured()
    override suspend fun setUserModerator(targetUid: String, moderator: Boolean): Result<Unit> = unconfigured()
    override suspend fun setUserSuspended(targetUid: String, suspended: Boolean): Result<Unit> = unconfigured()
    override suspend fun blockUser(targetUid: String): Result<Unit> = unconfigured()
    override suspend fun reportUser(targetUid: String, reason: String): Result<Unit> = unconfigured()
    override suspend fun deleteCurrentAccount(): Result<Unit> = unconfigured()
    override suspend fun fetchAuditLogs(): Result<List<AdminAuditLogEntry>> = unconfigured()
    override suspend fun fetchReports(): Result<List<UserReportRecord>> = unconfigured()
}
