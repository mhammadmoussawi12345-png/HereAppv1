package com.hereapp.messenger.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.hereapp.messenger.core.i18n.HereStrings

object HereRoutes {
    const val WELCOME = "welcome"
    const val SIGN_IN = "sign_in"
    const val CREATE_ACCOUNT = "create_account"
    const val PROFILE_SETUP = "profile_setup"

    // Main 5 Bottom Navigation Destinations
    const val CHATS = "chats"
    const val GROUPS = "groups"
    const val DISCOVER = "discover"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    // Secondary Hub, Messaging, Groups & Bookmarks
    const val CONTROL_CENTER = "control_center"
    const val CHAT_CONVERSATION = "chat_conversation"
    const val CREATE_GROUP = "create_group"
    const val BOOKMARKS = "bookmarks"
    const val MY_QR_CODE = "my_qr_code"
    const val SCAN_QR = "scan_qr"
}

enum class MainNavTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CHATS(
        route = HereRoutes.CHATS,
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
        testTag = "nav_tab_chats"
    ),
    GROUPS(
        route = HereRoutes.GROUPS,
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups,
        testTag = "nav_tab_groups"
    ),
    DISCOVER(
        route = HereRoutes.DISCOVER,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_tab_discover"
    ),
    PROFILE(
        route = HereRoutes.PROFILE,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline,
        testTag = "nav_tab_profile"
    ),
    SETTINGS(
        route = HereRoutes.SETTINGS,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    );

    fun label(strings: HereStrings): String = when (this) {
        CHATS -> strings.navChats
        GROUPS -> strings.navGroups
        DISCOVER -> strings.navDiscover
        PROFILE -> strings.navProfile
        SETTINGS -> strings.navSettings
    }
}
