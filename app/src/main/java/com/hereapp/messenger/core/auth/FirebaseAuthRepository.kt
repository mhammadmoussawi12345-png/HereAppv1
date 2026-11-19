package com.hereapp.messenger.core.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hereapp.messenger.R
import com.hereapp.messenger.core.identity.IdentityConstants
import com.hereapp.messenger.core.identity.UserProfile
import com.hereapp.messenger.ui.theme.HereAppAccent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthRepository(
    private val appContext: Context
) : AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        appContext.getString(R.string.firestore_database_id)
    )
    private val credentialManager: CredentialManager = CredentialManager.create(appContext)

    override val configStatus: BackendConfigStatus = BackendConfigInspector.inspect(appContext)

    override fun currentSession(): AuthSession? {
        val user = auth.currentUser ?: return null
        return AuthSession(
            firebaseUid = user.uid,
            email = user.email,
            emailVerified = user.isEmailVerified
        )
    }

    suspend fun loadCurrentProfile(): UserProfile? {
        val user = auth.currentUser ?: return null
        return try {
            val snap = db.collection("users").document(user.uid).get().await()
            if (!snap.exists()) return null
            UserProfile(
                firebaseUid = user.uid,
                username = snap.getString("username").orEmpty(),
                displayName = snap.getString("displayName").orEmpty(),
                biography = snap.getString("biography").orEmpty(),
                countryOrRegion = snap.getString("countryOrRegion").orEmpty(),
                photoUri = snap.getString("photoUri"),
                personalAccent = HereAppAccent.fromId(snap.getString("personalAccent")),
                hereAppId = snap.getString("userId") ?: snap.getString("hereAppId"),
                emailVerified = user.isEmailVerified,
                isVerified = snap.getBoolean("isVerified") == true,
                isOwner = snap.getBoolean("isOwner") == true,
                isAdmin = snap.getBoolean("isAdmin") == true,
                verifiedTier = snap.getString("verifiedTier"),
                badges = (snap.get("badges") as? List<*>)?.filterIsInstance<String>().orEmpty()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HereAuth", "Failed to load user profile", e)
            null
        }
    }

    override suspend fun signInWithEmailOrUsername(
        identifier: String,
        password: String
    ): AuthActionResult {
        return try {
            val trimmed = identifier.trim()
            val resolvedEmail = if (trimmed.contains("@") && trimmed.contains(".")) {
                trimmed
            } else {
                val normalized = IdentityConstants.normalizeUsername(trimmed)
                val lookup = db.collection("usernames").document(normalized).get().await()
                if (!lookup.exists()) {
                    return AuthActionResult.Error("No account found for @$normalized.")
                }
                lookup.getString("email")
                    ?: return AuthActionResult.Error("Account email could not be resolved for @$normalized.")
            }
            auth.signInWithEmailAndPassword(resolvedEmail, password).await()
            AuthActionResult.Success("Signed in successfully.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            Log.w("HereAuth", "Email/username sign-in error (${e.errorCode}): ${e.message}")
            val message = when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Email/Password sign-in is currently disabled in Firebase project gen-lang-client-0459405711. Enable it in Firebase Console → Authentication → Sign-in method, or use Continue with Google."
                "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
                    "Invalid email/username or password. If you signed up with Google, tap Continue with Google."
                else -> e.localizedMessage ?: "Sign in failed. Please check your credentials."
            }
            AuthActionResult.Error(message)
        } catch (e: Exception) {
            Log.w("HereAuth", "Email/username sign-in failed: ${e.message}")
            AuthActionResult.Error(e.localizedMessage ?: "Sign in failed. Please check your credentials.")
        }
    }

    override suspend fun signInWithGoogle(context: Context): AuthActionResult {
        val activity = context as? Activity
            ?: return AuthActionResult.Error("Google Sign-In requires an active Activity context.")
        val clientId = try {
            appContext.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return AuthActionResult.ConfigurationRequired(
                "Google Sign-In configuration missing: default_web_client_id not found."
            )
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                AuthActionResult.Success("Signed in with Google.")
            } else {
                AuthActionResult.Error("Unexpected credential type returned by Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("HereAuth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            AuthActionResult.Error("Google Sign-In was cancelled. Please select an account to continue.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("HereAuth", "Google Sign-In failed: ${e.message}")
            AuthActionResult.Error(e.localizedMessage ?: "Google Sign-In failed.")
        }
    }

    override suspend fun createAccount(email: String, password: String): AuthActionResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.sendEmailVerification()?.await()
            AuthActionResult.Success("Account created. A verification email has been sent to $email.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            Log.w("HereAuth", "Account creation collision: ${e.message}")
            AuthActionResult.Error(
                "This email is already registered. Please go to Sign In or use Continue with Google."
            )
        } catch (e: com.google.firebase.auth.FirebaseAuthException) {
            Log.w("HereAuth", "Account creation error (${e.errorCode}): ${e.message}")
            val message = when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Email/Password registration is disabled in Firebase project gen-lang-client-0459405711. Enable it in Firebase Console → Authentication → Sign-in method, or use Continue with Google."
                else -> e.localizedMessage ?: "Unable to create account."
            }
            AuthActionResult.Error(message)
        } catch (e: Exception) {
            Log.w("HereAuth", "Account creation failed: ${e.message}")
            AuthActionResult.Error(e.localizedMessage ?: "Unable to create account.")
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthActionResult.Success("Password reset link sent to $email.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HereAuth", "Password reset failed", e)
            AuthActionResult.Error(e.localizedMessage ?: "Unable to send password reset email.")
        }
    }

    override suspend fun sendEmailVerification(): AuthActionResult {
        val user = auth.currentUser
            ?: return AuthActionResult.Error("You must be signed in to send a verification email.")
        return try {
            user.sendEmailVerification().await()
            AuthActionResult.Success("Verification email sent to ${user.email}.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HereAuth", "Email verification failed", e)
            AuthActionResult.Error(e.localizedMessage ?: "Unable to send verification email.")
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile): AuthActionResult {
        val user = auth.currentUser
            ?: return AuthActionResult.Error("You must be signed in before saving your profile.")
        val normalizedUsername = IdentityConstants.normalizeUsername(profile.username)
        val usernameRef = db.collection("usernames").document(normalizedUsername)
        val userRef = db.collection("users").document(user.uid)
        val counterRef = db.collection("system").document("hereapp_id_counter")

        return try {
            val assignedHereAppId = db.runTransaction { tx ->
                val existingUsername = tx.get(usernameRef)
                if (existingUsername.exists() && existingUsername.getString("uid") != user.uid) {
                    throw IllegalStateException("The handle @$normalizedUsername is already taken.")
                }

                val existingUser = tx.get(userRef)
                val currentUserId = existingUser.getString("userId") ?: existingUser.getString("hereAppId")
                val finalUserId = if (!currentUserId.isNullOrBlank()) {
                    currentUserId
                } else {
                    val generatedId = IdentityConstants.generateRandomUserId()
                    val idRegistryRef = db.collection("hereapp_ids")
                        .document(IdentityConstants.registryKeyForId(generatedId))
                    tx.set(
                        idRegistryRef,
                        mapOf(
                            "uid" to user.uid,
                            "userId" to generatedId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    generatedId
                }

                val usernamePayload = mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "userId" to finalUserId,
                    "hereAppId" to finalUserId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(usernameRef, usernamePayload, SetOptions.merge())

                val userPayload = buildMap<String, Any> {
                    put("username", normalizedUsername)
                    put("displayName", profile.displayName)
                    put("biography", profile.biography)
                    put("countryOrRegion", profile.countryOrRegion)
                    put("personalAccent", profile.personalAccent.id)
                    put("userId", finalUserId)
                    put("hereAppId", finalUserId)
                    put("updatedAt", FieldValue.serverTimestamp())
                    if (profile.photoUri != null) {
                        put("photoUri", profile.photoUri)
                    }
                }
                tx.set(userRef, userPayload, SetOptions.merge())
                finalUserId
            }.await()

            AuthActionResult.Success("Profile saved as @$normalizedUsername (ID: $assignedHereAppId).")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("HereAuth", "Save profile failed: ${e.message}")
            AuthActionResult.Error(e.localizedMessage ?: "Unable to save profile.")
        }
    }

    override suspend fun signOut(): AuthActionResult {
        return withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("HereAuth", "Credential state clear warning: ${e.message}")
            } finally {
                auth.signOut()
            }
            AuthActionResult.Success("Signed out successfully.")
        }
    }
}
