package com.hereapp.messenger.core.identity

import com.hereapp.messenger.ui.theme.HereAppAccent

/**
 * HereApp Three-Identifier Identity Model.
 *
 * Strictly separates:
 * 1. [username] (@Username) — public, unique, user-chosen handle.
 * 2. [firebaseUid] — internal, private authentication UID. Never interchangeable with [hereAppId].
 * 3. [hereAppId] — optional application-level identifier (e.g., Founder ID "HR-DEV9KH"),
 *    assigned and linked exclusively through a verified backend process.
 */
data class UserProfile(
    val firebaseUid: String,
    val username: String,
    val displayName: String,
    val biography: String = "",
    val countryOrRegion: String = "",
    val photoUri: String? = null,
    val personalAccent: HereAppAccent = HereAppAccent.CHAMPAGNE_GOLD,
    val hereAppId: String? = null,
    val emailVerified: Boolean = false,
    val isVerified: Boolean = false,
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false,
    val verifiedTier: String? = null,
    val badges: List<String> = emptyList()
)

object IdentityConstants {
    /**
     * Reserved Founder HereApp IDs (HR-0001 primary, HR-DEV9KH legacy).
     * Bound exclusively to app_config/founder.ownerUid in Firestore.
     */
    const val FOUNDER_HEREAPP_ID = "HR-DEV9KH"
    const val OWNER_USER_ID = "HR-DEV9KH"
    const val LEGACY_FOUNDER_HEREAPP_ID = "HR-DEV9KH"
    const val FIRST_REGULAR_USER_SEQUENCE = 2L

    private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,24}$")
    private val HR_SLASH_ID_REGEX = Regex("^HR/\\d{6}$")
    private val HR_ID_REGEX = Regex("^HR-[A-Z0-9]{4,}$")
    private val NUMERIC_ID_REGEX = Regex("^\\d{5,}$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun generateRandomUserId(): String =
        "HR/${(100000..999999).random()}"

    fun normalizeUserId(raw: String): String =
        raw.trim().uppercase()

    fun isValidUserId(raw: String): Boolean {
        val cleaned = normalizeUserId(raw)
        return cleaned == OWNER_USER_ID || HR_SLASH_ID_REGEX.matches(cleaned)
    }

    fun registryKeyForId(id: String): String =
        normalizeUserId(id).replace("/", "_")

    fun formatHereAppId(sequenceNumber: Long): String =
        "HR-${sequenceNumber.toString().padStart(4, '0')}"

    fun formatNumericHereAppId(sequenceNumber: Long): String =
        formatHereAppId(sequenceNumber)

    fun isValidHrHereAppId(raw: String): Boolean {
        val cleaned = normalizeUserId(raw)
        return cleaned == OWNER_USER_ID ||
            HR_SLASH_ID_REGEX.matches(cleaned) ||
            HR_ID_REGEX.matches(cleaned)
    }

    fun isValidNumericHereAppId(raw: String): Boolean =
        NUMERIC_ID_REGEX.matches(raw.trim()) || isValidHrHereAppId(raw)

    fun normalizeUsername(raw: String): String =
        raw.trim().removePrefix("@").lowercase()

    fun isValidUsername(raw: String): Boolean =
        USERNAME_REGEX.matches(normalizeUsername(raw))

    fun isValidEmail(raw: String): Boolean =
        EMAIL_REGEX.matches(raw.trim())

    fun isValidPassword(password: String): Boolean =
        password.length >= 8
}
