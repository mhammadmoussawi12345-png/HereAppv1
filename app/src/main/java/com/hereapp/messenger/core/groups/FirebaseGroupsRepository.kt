package com.hereapp.messenger.core.groups

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.hereapp.messenger.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseGroupsRepository(
    appContext: Context
) : GroupsRepository {

    private val auth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        appContext.getString(R.string.firestore_database_id)
    )

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in before managing Here Groups.")

    override fun observeMyGroups(): Flow<List<HereGroup>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        db.collection("groups")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val memberUids = (doc.get("memberUids") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()
                    HereGroup(
                        id = doc.id,
                        name = name,
                        description = doc.getString("description").orEmpty(),
                        photoUri = doc.getString("photoUri"),
                        isPublic = doc.getBoolean("isPublic") == true,
                        members = memberUids.map { mUid ->
                            GroupMember(uid = mUid, username = mUid.take(8), displayName = mUid.take(8))
                        }
                    )
                }
            }
            .catch { err ->
                if (err is CancellationException) throw err
                Log.w("HereGroups", "observeMyGroups failed: ${err.message}")
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    override fun observePublicGroups(query: String): Flow<List<HereGroup>> = flow {
        if (auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        db.collection("groups")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                val trimmed = query.trim()
                snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val desc = doc.getString("description").orEmpty()
                    if (trimmed.isNotEmpty() &&
                        !name.contains(trimmed, ignoreCase = true) &&
                        !desc.contains(trimmed, ignoreCase = true)
                    ) {
                        return@mapNotNull null
                    }
                    val memberUids = (doc.get("memberUids") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()
                    HereGroup(
                        id = doc.id,
                        name = name,
                        description = desc,
                        photoUri = doc.getString("photoUri"),
                        isPublic = true,
                        members = memberUids.map { mUid ->
                            GroupMember(uid = mUid, username = mUid.take(8), displayName = mUid.take(8))
                        }
                    )
                }
            }
            .catch { err ->
                if (err is CancellationException) throw err
                Log.w("HereGroups", "observePublicGroups failed: ${err.message}")
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    override suspend fun createGroup(
        name: String,
        description: String,
        photoUri: String?,
        isPublic: Boolean,
        permissions: GroupPermissions
    ): Result<HereGroup> {
        return try {
            val uid = requireUid()
            val docRef = db.collection("groups").document()
            val payload = buildMap<String, Any> {
                put("ownerUid", uid)
                put("name", name)
                put("description", description)
                put("isPublic", isPublic)
                put("memberUids", listOf(uid))
                put("adminUids", listOf(uid))
                put("membersCanSendMessages", permissions.membersCanSendMessages)
                put("membersCanAddOthers", permissions.membersCanAddOthers)
                put("moderatorsCanPinMessages", permissions.moderatorsCanPinMessages)
                put("moderatorsCanRemoveMembers", permissions.moderatorsCanRemoveMembers)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
                if (photoUri != null) put("photoUri", photoUri)
            }
            docRef.set(payload).await()
            Result.success(
                HereGroup(
                    id = docRef.id,
                    name = name,
                    description = description,
                    photoUri = photoUri,
                    isPublic = isPublic,
                    permissions = permissions
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("HereGroups", "createGroup failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(groupId: String): Result<Unit> {
        return try {
            val uid = requireUid()
            db.collection("groups").document(groupId)
                .update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val uid = requireUid()
            db.collection("groups").document(groupId)
                .update(
                    mapOf(
                        "memberUids" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMember(groupId: String, username: String): Result<Unit> {
        return try {
            requireUid()
            val lookup = db.collection("usernames").document(username.trim().lowercase()).get().await()
            val targetUid = lookup.getString("uid")
                ?: return Result.failure(IllegalArgumentException("User @$username not found."))
            db.collection("groups").document(groupId)
                .update("memberUids", FieldValue.arrayUnion(targetUid))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(groupId: String, memberUid: String): Result<Unit> {
        return try {
            requireUid()
            db.collection("groups").document(groupId)
                .update("memberUids", FieldValue.arrayRemove(memberUid))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemberRole(groupId: String, memberUid: String, role: GroupRole): Result<Unit> {
        return try {
            requireUid()
            val op = if (role == GroupRole.ADMIN || role == GroupRole.OWNER) {
                FieldValue.arrayUnion(memberUid)
            } else {
                FieldValue.arrayRemove(memberUid)
            }
            db.collection("groups").document(groupId)
                .update("adminUids", op)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
