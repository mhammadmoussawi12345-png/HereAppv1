package com.hereapp.messenger.core.security

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.hereapp.messenger.R
import com.hereapp.messenger.core.identity.IdentityConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class FounderConfig(
    val ownerUid: String = "",
    val role: String = "OWNER",
    val locked: Boolean = false
)

data class VerificationRequestItem(
    val id: String,
    val uid: String,
    val hereAppId: String,
    val username: String,
    val displayName: String,
    val reason: String,
    val status: String
)

data class OwnerGroupSummary(
    val id: String,
    val name: String,
    val memberCount: Int,
    val isPublic: Boolean
)

data class FoundUserProfile(
    val uid: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val isVerified: Boolean
)

class FirebaseVerificationRepository(
    appContext: Context
) {
    private val auth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        appContext.getString(R.string.firestore_database_id)
    )

    suspend fun getFounderConfig(): FounderConfig? {
        return try {
            val snap = db.collection("app_config").document("founder").get().await()
            if (!snap.exists()) return null
            FounderConfig(
                ownerUid = snap.getString("ownerUid").orEmpty(),
                role = snap.getString("role") ?: "OWNER",
                locked = snap.getBoolean("locked") == true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("HereVerify", "getFounderConfig failed: ${e.message}")
            null
        }
    }

    suspend fun isCurrentUserFounderOwner(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val config = getFounderConfig() ?: return false
        return config.locked && config.ownerUid == uid
    }

    /**
     * One-time locked Founder bootstrap: binds current authenticated UID to app_config/founder
     * and sets permanent HR-0001 + isOwner + isVerified on the Founder profile.
     */
    suspend fun bindFounderAccountOnce(): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            val founderRef = db.collection("app_config").document("founder")
            founderRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "role" to "OWNER",
                    "locked" to true,
                    "founderHereAppId" to IdentityConstants.FOUNDER_HEREAPP_ID,
                    "boundAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("hereapp_ids").document(IdentityConstants.FOUNDER_HEREAPP_ID)
                .set(mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()

            db.collection("users").document(uid)
                .set(
                    mapOf(
                        "userId" to IdentityConstants.OWNER_USER_ID,
                        "hereAppId" to IdentityConstants.OWNER_USER_ID,
                        "legacyHereAppId" to IdentityConstants.LEGACY_FOUNDER_HEREAPP_ID,
                        "isOwner" to true,
                        "isAdmin" to true,
                        "isVerified" to true,
                        "verifiedTier" to "official",
                        "badges" to listOf("founder", "owner", "verified"),
                        "role" to "OWNER",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()

            Result.success("Owner account bound permanently (ID: ${IdentityConstants.OWNER_USER_ID}).")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * User submits a verification request stored securely in /verification_requests/{uid}.
     */
    suspend fun submitVerificationRequest(
        hereAppId: String,
        username: String,
        displayName: String,
        reason: String
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Please sign in first."))
        if (reason.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Please provide a brief reason for verification."))
        }
        return try {
            db.collection("verification_requests").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "hereAppId" to hereAppId,
                    "username" to username,
                    "displayName" to displayName,
                    "reason" to reason.trim(),
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success("Verification request submitted for review.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of pending verification requests (Owner-only via firestore.rules).
     */
    fun observePendingRequests(): Flow<List<VerificationRequestItem>> = flow {
        if (auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        db.collection("verification_requests")
            .whereEqualTo("status", "PENDING")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: return@mapNotNull null
                    VerificationRequestItem(
                        id = doc.id,
                        uid = uid,
                        hereAppId = doc.getString("hereAppId").orEmpty(),
                        username = doc.getString("username").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty(),
                        reason = doc.getString("reason").orEmpty(),
                        status = doc.getString("status") ?: "PENDING"
                    )
                }
            }
            .catch { err ->
                if (err is CancellationException) throw err
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    /**
     * Owner approves a pending verification request.
     */
    suspend fun approveRequest(request: VerificationRequestItem): Result<String> {
        val ownerUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Owner authentication required."))
        return try {
            db.collection("users").document(request.uid)
                .set(
                    mapOf(
                        "isVerified" to true,
                        "verifiedTier" to "official",
                        "badges" to FieldValue.arrayUnion("verified"),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
            db.collection("verification_requests").document(request.id)
                .update(mapOf("status" to "APPROVED", "reviewedBy" to ownerUid, "reviewedAt" to FieldValue.serverTimestamp()))
                .await()
            sendUserNotification(
                targetUid = request.uid,
                title = "Account Verified",
                body = "Your HereApp verification request was approved. The blue verified badge is now active on your profile."
            )
            logAdminAction(ownerUid, "APPROVE_VERIFICATION", request.hereAppId.ifBlank { request.uid })
            Result.success("Approved verification for ${request.displayName}.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Owner rejects a pending verification request and sends a notification to the user.
     */
    suspend fun rejectRequest(request: VerificationRequestItem, reason: String): Result<String> {
        val ownerUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Owner authentication required."))
        return try {
            db.collection("verification_requests").document(request.id)
                .update(
                    mapOf(
                        "status" to "REJECTED",
                        "rejectionReason" to reason.trim(),
                        "reviewedBy" to ownerUid,
                        "reviewedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            sendUserNotification(
                targetUid = request.uid,
                title = "Verification Request Declined",
                body = reason.trim().ifBlank { "Your verification request was not approved at this time." }
            )
            logAdminAction(ownerUid, "REJECT_VERIFICATION", request.hereAppId.ifBlank { request.uid })
            Result.success("Rejected verification request and notified user.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Owner-only: Search for a user by HereApp ID or @Username.
     */
    suspend fun findUserByUserId(rawUserId: String): Result<FoundUserProfile?> {
        val trimmed = rawUserId.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a HereApp ID or @username."))
        }
        return try {
            val doc = if (trimmed.startsWith("@") || (!trimmed.uppercase().startsWith("HR") && !trimmed.all { it.isDigit() })) {
                val normalizedUsername = IdentityConstants.normalizeUsername(trimmed)
                db.collection("users")
                    .whereEqualTo("username", normalizedUsername)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            } else {
                val normalizedId = IdentityConstants.normalizeUserId(trimmed)
                db.collection("users").whereEqualTo("userId", normalizedId).limit(1).get().await().documents.firstOrNull()
                    ?: db.collection("users").whereEqualTo("hereAppId", normalizedId).limit(1).get().await().documents.firstOrNull()
            }

            if (doc == null) {
                Result.success(null)
            } else {
                Result.success(
                    FoundUserProfile(
                        uid = doc.id,
                        userId = doc.getString("userId") ?: doc.getString("hereAppId").orEmpty(),
                        username = doc.getString("username").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty().ifBlank { "HereApp User" },
                        isVerified = doc.getBoolean("isVerified") == true
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Owner-only: Toggle isVerified directly on a found user document.
     */
    suspend fun setVerifiedByUid(targetUid: String, userId: String, verified: Boolean): Result<String> {
        val ownerUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Owner authentication required."))
        return try {
            val badgeUpdate: Any = if (verified) FieldValue.arrayUnion("verified") else FieldValue.arrayRemove("verified")
            db.collection("users").document(targetUid)
                .set(
                    mapOf(
                        "isVerified" to verified,
                        "verifiedTier" to if (verified) "official" else "",
                        "badges" to badgeUpdate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
            sendUserNotification(
                targetUid = targetUid,
                title = if (verified) "Account Verified" else "Verification Removed",
                body = if (verified) {
                    "Your HereApp account ($userId) has been verified."
                } else {
                    "Your verified badge on $userId was removed by the Owner."
                }
            )
            logAdminAction(ownerUid, if (verified) "VERIFY_USER" else "REMOVE_VERIFICATION", userId)
            Result.success(if (verified) "✅ Verified $userId." else "❌ Removed verification from $userId.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct Verification / Revocation STRICTLY by HereApp ID (HR/XXXXXX or 1000000000).
     */
    suspend fun setVerifiedByHereAppId(
        rawHereAppId: String,
        verified: Boolean,
        reason: String = ""
    ): Result<String> {
        val ownerUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Owner authentication required."))
        val normalizedId = IdentityConstants.normalizeUserId(rawHereAppId)
        if (!IdentityConstants.isValidHrHereAppId(normalizedId)) {
            return Result.failure(
                IllegalArgumentException(
                    "Invalid format. Enter a valid User ID (HR/XXXXXX or 1000000000)."
                )
            )
        }

        return try {
            val idDoc = db.collection("hereapp_ids")
                .document(IdentityConstants.registryKeyForId(normalizedId))
                .get()
                .await()
            val targetUid = idDoc.getString("uid")
                ?: db.collection("users").whereEqualTo("userId", normalizedId).limit(1).get().await().documents.firstOrNull()?.id
                ?: return Result.failure(IllegalStateException("No account found for ID $normalizedId."))

            setVerifiedByUid(targetUid, normalizedId, verified)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Owner-only: Appoint or remove an Admin strictly by HereApp ID (HR-XXXX).
     */
    suspend fun setAdminByHereAppId(rawHereAppId: String, isAdmin: Boolean): Result<String> {
        val ownerUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Owner authentication required."))
        val normalizedId = rawHereAppId.trim().uppercase()
        if (!IdentityConstants.isValidHrHereAppId(normalizedId)) {
            return Result.failure(IllegalArgumentException("Enter a valid HereApp ID (HR-XXXX)."))
        }
        return try {
            val idDoc = db.collection("hereapp_ids").document(normalizedId).get().await()
            val targetUid = idDoc.getString("uid")
                ?: return Result.failure(IllegalStateException("No account found for HereApp ID $normalizedId."))

            db.collection("users").document(targetUid)
                .set(
                    mapOf(
                        "isAdmin" to isAdmin,
                        "role" to if (isAdmin) "ADMIN" else "USER",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()

            sendUserNotification(
                targetUid = targetUid,
                title = if (isAdmin) "Admin Role Granted" else "Admin Role Removed",
                body = if (isAdmin) {
                    "You have been appointed as a HereApp Admin by the Founder."
                } else {
                    "Your HereApp Admin privileges have been removed by the Founder."
                }
            )
            logAdminAction(ownerUid, if (isAdmin) "APPOINT_ADMIN" else "REMOVE_ADMIN", normalizedId)
            Result.success(if (isAdmin) "Appointed $normalizedId as Admin." else "Removed Admin role from $normalizedId.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Owner-only: Real-time stream of all groups in Firestore for moderation.
     */
    fun observeAllGroups(): Flow<List<OwnerGroupSummary>> = flow {
        if (auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        db.collection("groups")
            .snapshots()
            .map { snap ->
                snap.documents.map { doc ->
                    val members = (doc.get("memberUids") as? List<*>)?.size ?: 0
                    OwnerGroupSummary(
                        id = doc.id,
                        name = doc.getString("name").orEmpty().ifBlank { "Unnamed Group" },
                        memberCount = members,
                        isPublic = doc.getBoolean("isPublic") == true
                    )
                }
            }
            .catch { err ->
                if (err is CancellationException) throw err
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    /**
     * Owner-only: Rename any group.
     */
    suspend fun renameGroupAsOwner(groupId: String, newName: String): Result<String> {
        val ownerUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Owner authentication required."))
        val trimmed = newName.trim()
        if (trimmed.length < 2) {
            return Result.failure(IllegalArgumentException("Group name must be at least 2 characters."))
        }
        return try {
            db.collection("groups").document(groupId)
                .update(mapOf("name" to trimmed, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            logAdminAction(ownerUid, "OWNER_RENAME_GROUP", "$groupId -> $trimmed")
            Result.success("Group renamed to \"$trimmed\".")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Owner-only: Delete any group.
     */
    suspend fun deleteGroupAsOwner(groupId: String, groupName: String): Result<String> {
        val ownerUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Owner authentication required."))
        return try {
            db.collection("groups").document(groupId).delete().await()
            logAdminAction(ownerUid, "OWNER_DELETE_GROUP", "$groupId ($groupName)")
            Result.success("Group \"$groupName\" deleted.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendUserNotification(targetUid: String, title: String, body: String) {
        db.collection("users").document(targetUid).collection("notifications").add(
            mapOf(
                "title" to title,
                "body" to body,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend fun logAdminAction(actorUid: String, action: String, targetIdentifier: String) {
        db.collection("audit_logs").add(
            mapOf(
                "actorUid" to actorUid,
                "action" to action,
                "targetIdentifier" to targetIdentifier,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
