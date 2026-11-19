package com.hereapp.messenger.core.auth

import android.content.Context
import com.hereapp.messenger.core.identity.UserProfile

data class AuthSession(
    val firebaseUid: String,
    val email: String?,
    val emailVerified: Boolean,
    val profile: UserProfile? = null
)

sealed interface AuthActionResult {
    data class Success(val message: String) : AuthActionResult
    data class ConfigurationRequired(val details: String) : AuthActionResult
    data class Error(val message: String) : AuthActionResult
}

data class BackendConfigStatus(
    val hasGoogleServicesConfig: Boolean,
    val hasWebClientId: Boolean,
    val packageName: String
) {
    val isFirebaseReady: Boolean
        get() = hasGoogleServicesConfig && hasWebClientId
}

object BackendConfigInspector {
    fun inspect(context: Context): BackendConfigStatus {
        val pkg = context.packageName
        val appIdRes = context.resources.getIdentifier("google_app_id", "string", pkg)
        val webClientRes = context.resources.getIdentifier("default_web_client_id", "string", pkg)
        return BackendConfigStatus(
            hasGoogleServicesConfig = appIdRes != 0,
            hasWebClientId = webClientRes != 0,
            packageName = pkg
        )
    }
}

interface AuthRepository {
    val configStatus: BackendConfigStatus
    fun currentSession(): AuthSession?
    suspend fun signInWithEmailOrUsername(identifier: String, password: String): AuthActionResult
    suspend fun signInWithGoogle(context: Context): AuthActionResult
    suspend fun createAccount(email: String, password: String): AuthActionResult
    suspend fun sendPasswordReset(email: String): AuthActionResult
    suspend fun sendEmailVerification(): AuthActionResult
    suspend fun saveUserProfile(profile: UserProfile): AuthActionResult
    suspend fun signOut(): AuthActionResult
}

/**
 * Honest fail-closed repository used when Firebase / Google OAuth has not yet been provisioned.
 * Strictly NEVER simulates successful authentication.
 */
class UnprovisionedAuthRepository(
    context: Context
) : AuthRepository {

    override val configStatus: BackendConfigStatus = BackendConfigInspector.inspect(context)

    private val missingConfigMessage: String =
        "Firebase Authentication & Google OAuth are not provisioned yet. " +
            "Enable Firebase setup or add app/google-services.json with SHA-1/SHA-256 registered to activate live authentication."

    override fun currentSession(): AuthSession? = null

    override suspend fun signInWithEmailOrUsername(identifier: String, password: String): AuthActionResult =
        AuthActionResult.ConfigurationRequired(missingConfigMessage)

    override suspend fun signInWithGoogle(context: Context): AuthActionResult =
        AuthActionResult.ConfigurationRequired(missingConfigMessage)

    override suspend fun createAccount(email: String, password: String): AuthActionResult =
        AuthActionResult.ConfigurationRequired(missingConfigMessage)

    override suspend fun sendPasswordReset(email: String): AuthActionResult =
        AuthActionResult.ConfigurationRequired(missingConfigMessage)

    override suspend fun sendEmailVerification(): AuthActionResult =
        AuthActionResult.ConfigurationRequired(missingConfigMessage)

    override suspend fun saveUserProfile(profile: UserProfile): AuthActionResult =
        AuthActionResult.ConfigurationRequired(missingConfigMessage)

    override suspend fun signOut(): AuthActionResult =
        AuthActionResult.Success("No active session to sign out.")
}
