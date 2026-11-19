package com.hereapp.messenger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hereapp.messenger.core.identity.IdentityConstants
import com.hereapp.messenger.core.identity.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HereApp", appName)
    }

    @Test
    fun `generated user IDs match HR slash 6-digit format and sanitize safely for Firestore`() {
        repeat(25) {
            val id = IdentityConstants.generateRandomUserId()
            assertTrue("Expected HR/XXXXXX format, got $id", IdentityConstants.isValidUserId(id))
            val key = IdentityConstants.registryKeyForId(id)
            assertFalse("Firestore document key must not contain '/'", key.contains("/"))
            assertTrue(key.startsWith("HR_"))
        }
    }

    @Test
    fun `founder ID is HR-DEV9KH and invalid IDs are rejected`() {
        assertEquals("HR-DEV9KH", IdentityConstants.FOUNDER_HEREAPP_ID)
        assertTrue(IdentityConstants.isValidUserId("HR-DEV9KH"))
        assertTrue(IdentityConstants.isValidUserId("HR/482917"))
        assertFalse(IdentityConstants.isValidUserId("HR/123"))
        assertFalse(IdentityConstants.isValidUserId("random_username"))
    }

    @Test
    fun `default user profile is never verified or owner`() {
        val profile = UserProfile(
            firebaseUid = "uid_123",
            username = "testuser",
            displayName = "Test User"
        )
        assertFalse(profile.isVerified)
        assertFalse(profile.isOwner)
        assertFalse(profile.isAdmin)
    }
}
