package com.hereapp.messenger.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereapp.messenger.core.i18n.HereLocalization
import com.hereapp.messenger.core.i18n.LocalHereStrings
import com.hereapp.messenger.navigation.HereRoutes
import com.hereapp.messenger.navigation.MainNavTab
import com.hereapp.messenger.ui.components.LanguageSelectorSheet
import com.hereapp.messenger.ui.screens.ChatsScreen
import com.hereapp.messenger.ui.screens.ControlCenterScreen
import com.hereapp.messenger.ui.screens.CreateAccountScreen
import com.hereapp.messenger.ui.screens.DiscoverScreen
import com.hereapp.messenger.ui.screens.GroupsScreen
import com.hereapp.messenger.ui.screens.ProfileScreen
import com.hereapp.messenger.ui.screens.ProfileSetupScreen
import com.hereapp.messenger.ui.screens.SettingsScreen
import com.hereapp.messenger.ui.screens.SignInScreen
import com.hereapp.messenger.ui.screens.WelcomeScreen
import com.hereapp.messenger.ui.theme.HereAppTheme
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors

@Composable
fun HereAppRoot(
    viewModel: AppViewModel,
    authViewModel: com.hereapp.messenger.ui.auth.AuthViewModel,
    messagingViewModel: com.hereapp.messenger.ui.messaging.MessagingViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val messagingState by messagingViewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = remember(uiState.selectedLanguage.code) {
        HereLocalization.stringsFor(uiState.selectedLanguage.code)
    }
    val layoutDirection = if (uiState.selectedLanguage.isRtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    var showLanguageSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )

    androidx.compose.runtime.LaunchedEffect(authState.session?.firebaseUid) {
        com.hereapp.messenger.core.notifications.FcmTokenManager.ensureNotificationChannel(context)
        if (authState.session != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            com.hereapp.messenger.core.notifications.FcmTokenManager.syncCurrentToken(context)
        }
    }

    HereAppTheme(
        accent = uiState.selectedAccent,
        secondaryAccent = uiState.secondaryAccent,
        isDarkMode = uiState.isDarkMode
    ) {
        CompositionLocalProvider(
            LocalHereStrings provides strings,
            LocalLayoutDirection provides layoutDirection
        ) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val mainRoutes = remember { MainNavTab.entries.map { it.route }.toSet() }
            val showBottomBar = currentRoute in mainRoutes
            val luxuryColors = LocalHereLuxuryColors.current
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

            androidx.compose.runtime.LaunchedEffect(currentRoute, showLanguageSheet) {
                focusManager.clearFocus(force = true)
            }

            androidx.compose.runtime.LaunchedEffect(authState.navigationTarget) {
                when (authState.navigationTarget) {
                    com.hereapp.messenger.ui.auth.AuthNavEvent.TO_WORKSPACE -> {
                        navController.navigate(HereRoutes.CHATS) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                        authViewModel.consumeNavigationEvent()
                    }
                    com.hereapp.messenger.ui.auth.AuthNavEvent.TO_PROFILE_SETUP -> {
                        navController.navigate(HereRoutes.PROFILE_SETUP) {
                            launchSingleTop = true
                        }
                        authViewModel.consumeNavigationEvent()
                    }
                    com.hereapp.messenger.ui.auth.AuthNavEvent.TO_WELCOME -> {
                        navController.navigate(HereRoutes.WELCOME) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                        authViewModel.consumeNavigationEvent()
                    }
                    null -> Unit
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = if (showBottomBar) WindowInsets.statusBars else WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) {
                        Box {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets.navigationBars,
                                modifier = Modifier.testTag("main_bottom_navigation")
                            ) {
                                MainNavTab.entries.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (!selected) {
                                                navController.navigate(tab.route) {
                                                    popUpTo(HereRoutes.CHATS) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label(strings)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label(strings),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = luxuryColors.subtleDivider
                            )
                        }
                    }
                }
            ) { innerPadding ->
                val initialDestination = remember {
                    if (authState.session != null) HereRoutes.CHATS else HereRoutes.WELCOME
                }
                NavHost(
                    navController = navController,
                    startDestination = initialDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(HereRoutes.WELCOME) {
                        WelcomeScreen(
                            currentLanguage = uiState.selectedLanguage,
                            onOpenLanguageSelector = { showLanguageSheet = true },
                            onSignInClick = { navController.navigate(HereRoutes.SIGN_IN) },
                            onCreateAccountClick = { navController.navigate(HereRoutes.CREATE_ACCOUNT) },
                            onExploreWorkspaceClick = {
                                navController.navigate(HereRoutes.CHATS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(HereRoutes.SIGN_IN) {
                        SignInScreen(
                            authState = authState,
                            onClearFeedback = authViewModel::clearFeedback,
                            onBack = { navController.popBackStack() },
                            onSignIn = authViewModel::signIn,
                            onGoogleSignIn = { authViewModel.signInWithGoogle(context) },
                            onForgotPassword = authViewModel::sendPasswordReset
                        )
                    }

                    composable(HereRoutes.CREATE_ACCOUNT) {
                        CreateAccountScreen(
                            authState = authState,
                            onClearFeedback = authViewModel::clearFeedback,
                            onBack = { navController.popBackStack() },
                            onCreateAccount = authViewModel::createAccount,
                            onOpenProfileSetup = {
                                navController.navigate(HereRoutes.PROFILE_SETUP)
                            },
                            onOpenSignIn = {
                                navController.navigate(HereRoutes.SIGN_IN) {
                                    popUpTo(HereRoutes.CREATE_ACCOUNT) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(HereRoutes.PROFILE_SETUP) {
                        ProfileSetupScreen(
                            authState = authState,
                            selectedAccent = uiState.selectedAccent,
                            onSelectAccent = viewModel::selectAccent,
                            onClearFeedback = authViewModel::clearFeedback,
                            onBack = { navController.popBackStack() },
                            onSaveProfile = authViewModel::saveProfile
                        )
                    }

                    composable(HereRoutes.CHATS) {
                        ChatsScreen(
                            showArchived = messagingState.showArchived,
                            onToggleArchived = messagingViewModel::setShowArchived
                        )
                    }

                    composable(HereRoutes.CHAT_CONVERSATION) {
                        com.hereapp.messenger.ui.screens.ChatConversationScreen(
                            uiState = messagingState,
                            currentUserUid = authState.session?.firebaseUid.orEmpty(),
                            onBack = { navController.popBackStack() },
                            onSendOrEdit = messagingViewModel::sendOrEditMessage,
                            onStartReply = messagingViewModel::startReply,
                            onStartEdit = messagingViewModel::startEdit,
                            onClearComposerContext = messagingViewModel::clearComposerContext,
                            onDeleteMessage = messagingViewModel::deleteMessage,
                            onToggleReaction = messagingViewModel::toggleReaction,
                            onTogglePin = messagingViewModel::togglePin,
                            onArchiveConversation = messagingViewModel::setArchived
                        )
                    }

                    composable(HereRoutes.GROUPS) {
                        val groupsRepo = remember {
                            com.hereapp.messenger.core.groups.FirebaseGroupsRepository(context.applicationContext)
                        }
                        GroupsScreen(
                            groupsRepository = groupsRepo,
                            currentUserUid = authState.session?.firebaseUid.orEmpty(),
                            onCreateGroupClick = { navController.navigate(HereRoutes.CREATE_GROUP) }
                        )
                    }

                    composable(HereRoutes.CREATE_GROUP) {
                        val groupsRepo = remember {
                            com.hereapp.messenger.core.groups.FirebaseGroupsRepository(context.applicationContext)
                        }
                        com.hereapp.messenger.ui.screens.CreateGroupScreen(
                            groupsRepository = groupsRepo,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(HereRoutes.DISCOVER) {
                        DiscoverScreen()
                    }

                    composable(HereRoutes.PROFILE) {
                        ProfileScreen(
                            authSession = authState.session,
                            onOpenProfileSetup = { navController.navigate(HereRoutes.PROFILE_SETUP) },
                            onOpenMyQr = { navController.navigate(HereRoutes.MY_QR_CODE) },
                            onOpenScanQr = { navController.navigate(HereRoutes.SCAN_QR) },
                            onOpenBookmarks = { navController.navigate(HereRoutes.BOOKMARKS) },
                            onOpenControlCenter = { navController.navigate(HereRoutes.CONTROL_CENTER) },
                            onSignOut = authViewModel::signOut
                        )
                    }

                    composable(HereRoutes.MY_QR_CODE) {
                        com.hereapp.messenger.ui.screens.MyQrCodeScreen(
                            authSession = authState.session,
                            onBack = { navController.popBackStack() },
                            onOpenScanner = { navController.navigate(HereRoutes.SCAN_QR) }
                        )
                    }

                    composable(HereRoutes.SCAN_QR) {
                        com.hereapp.messenger.ui.screens.ScanQrScreen(
                            onBack = { navController.popBackStack() },
                            onConfirmedStartChat = { _, _, _ ->
                                navController.navigate(HereRoutes.CHATS) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(HereRoutes.BOOKMARKS) {
                        val bookmarksRepo = remember { com.hereapp.messenger.core.bookmarks.UnprovisionedBookmarkRepository() }
                        com.hereapp.messenger.ui.screens.BookmarksScreen(
                            bookmarkRepository = bookmarksRepo,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(HereRoutes.SETTINGS) {
                        SettingsScreen(
                            currentLanguage = uiState.selectedLanguage,
                            selectedAccent = uiState.selectedAccent,
                            secondaryAccent = uiState.secondaryAccent,
                            isDarkMode = uiState.isDarkMode,
                            onSelectAccent = viewModel::selectAccent,
                            onApplyTheme = viewModel::applyTheme,
                            onOpenLanguageSelector = { showLanguageSheet = true },
                            onOpenControlCenter = { navController.navigate(HereRoutes.CONTROL_CENTER) },
                            onSignOut = authViewModel::signOut,
                            onReturnToWelcome = {
                                navController.navigate(HereRoutes.WELCOME) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(HereRoutes.CONTROL_CENTER) {
                        ControlCenterScreen(
                            currentLanguage = uiState.selectedLanguage,
                            selectedAccent = uiState.selectedAccent,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            if (showLanguageSheet) {
                LanguageSelectorSheet(
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectLanguage = { lang -> viewModel.selectLanguage(lang.code) },
                    onDismiss = { showLanguageSheet = false }
                )
            }
        }
    }
}
