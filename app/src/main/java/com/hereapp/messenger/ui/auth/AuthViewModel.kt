package com.hereapp.messenger.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.hereapp.messenger.core.auth.AuthActionResult
import com.hereapp.messenger.core.auth.AuthRepository
import com.hereapp.messenger.core.auth.AuthSession
import com.hereapp.messenger.core.auth.BackendConfigStatus
import com.hereapp.messenger.core.auth.FirebaseAuthRepository
import com.hereapp.messenger.core.identity.IdentityConstants
import com.hereapp.messenger.core.identity.UserProfile
import com.hereapp.messenger.ui.theme.HereAppAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val session: AuthSession? = null,
    val configStatus: BackendConfigStatus = BackendConfigStatus(false, false, ""),
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val navigationTarget: AuthNavEvent? = null
)

enum class AuthNavEvent {
    TO_WORKSPACE,
    TO_PROFILE_SETUP,
    TO_WELCOME
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            session = authRepository.currentSession(),
            configStatus = authRepository.configStatus
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener {
        refreshSessionAndProfile()
    }

    init {
        Firebase.auth.addAuthStateListener(authStateListener)
        refreshSessionAndProfile()
    }

    override fun onCleared() {
        super.onCleared()
        Firebase.auth.removeAuthStateListener(authStateListener)
    }

    fun consumeNavigationEvent() {
        _uiState.update { it.copy(navigationTarget = null) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, isError = false) }
    }

    private fun refreshSessionAndProfile() {
        val baseSession = authRepository.currentSession()
        if (baseSession == null) {
            _uiState.update { it.copy(session = null) }
            return
        }
        _uiState.update { it.copy(session = baseSession) }
        if (authRepository is FirebaseAuthRepository) {
            viewModelScope.launch {
                val profile = authRepository.loadCurrentProfile()
                _uiState.update { state ->
                    state.copy(session = baseSession.copy(profile = profile))
                }
            }
        }
    }

    fun signIn(identifier: String, password: String) {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) {
            setError("Please enter your email address or @username.")
            return
        }
        if (password.isEmpty()) {
            setError("Please enter your password.")
            return
        }
        runAuthAction(
            block = { authRepository.signInWithEmailOrUsername(trimmed, password) },
            onSuccessNav = { resolvePostAuthDestination() }
        )
    }

    fun signInWithGoogle(context: Context) {
        runAuthAction(
            block = { authRepository.signInWithGoogle(context) },
            onSuccessNav = { resolvePostAuthDestination() }
        )
    }

    fun createAccount(
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ) {
        if (!IdentityConstants.isValidEmail(email)) {
            setError("Please enter a valid email address.")
            return
        }
        if (!IdentityConstants.isValidPassword(password)) {
            setError("Password must be at least 8 characters.")
            return
        }
        if (password != confirmPassword) {
            setError("Passwords do not match.")
            return
        }
        if (!acceptedTerms) {
            setError("You must accept the Terms of Service and Privacy Policy.")
            return
        }
        runAuthAction(
            block = { authRepository.createAccount(email.trim(), password) },
            onSuccessNav = { AuthNavEvent.TO_PROFILE_SETUP }
        )
    }

    fun sendPasswordReset(email: String) {
        if (!IdentityConstants.isValidEmail(email)) {
            setError("Enter a valid email address to receive a password reset link.")
            return
        }
        runAuthAction(block = { authRepository.sendPasswordReset(email.trim()) })
    }

    fun saveProfile(
        username: String,
        displayName: String,
        biography: String,
        countryOrRegion: String,
        photoUri: String?,
        personalAccent: HereAppAccent
    ) {
        if (!IdentityConstants.isValidUsername(username)) {
            setError("@Username must be 3–24 characters using lowercase letters, numbers, or underscores.")
            return
        }
        if (displayName.trim().isEmpty()) {
            setError("Display name is required.")
            return
        }
        val uid = authRepository.currentSession()?.firebaseUid.orEmpty()
        val profile = UserProfile(
            firebaseUid = uid,
            username = IdentityConstants.normalizeUsername(username),
            displayName = displayName.trim(),
            biography = biography.trim(),
            countryOrRegion = countryOrRegion.trim(),
            photoUri = photoUri,
            personalAccent = personalAccent
        )
        runAuthAction(
            block = { authRepository.saveUserProfile(profile) },
            onSuccessNav = { AuthNavEvent.TO_WORKSPACE }
        )
    }

    fun signOut() {
        runAuthAction(
            block = { authRepository.signOut() },
            onSuccessNav = { AuthNavEvent.TO_WELCOME }
        )
    }

    private suspend fun resolvePostAuthDestination(): AuthNavEvent {
        val profile = if (authRepository is FirebaseAuthRepository) {
            authRepository.loadCurrentProfile()
        } else {
            null
        }
        val session = authRepository.currentSession()?.copy(profile = profile)
        _uiState.update { it.copy(session = session) }
        return if (profile != null && profile.username.isNotBlank()) {
            AuthNavEvent.TO_WORKSPACE
        } else {
            AuthNavEvent.TO_PROFILE_SETUP
        }
    }

    private fun runAuthAction(
        block: suspend () -> AuthActionResult,
        onSuccessNav: (suspend () -> AuthNavEvent)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = null, isError = false) }
            when (val result = block()) {
                is AuthActionResult.Success -> {
                    val nav = onSuccessNav?.invoke()
                    refreshSessionAndProfile()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feedbackMessage = result.message,
                            isError = false,
                            navigationTarget = nav
                        )
                    }
                }
                is AuthActionResult.ConfigurationRequired -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = result.details,
                        isError = true
                    )
                }
                is AuthActionResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(feedbackMessage = message, isError = true) }
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
