package com.hereapp.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hereapp.messenger.core.i18n.LocalHereStrings
import com.hereapp.messenger.core.i18n.SupportedLanguage
import com.hereapp.messenger.core.identity.IdentityConstants
import com.hereapp.messenger.core.security.FirebaseVerificationRepository
import com.hereapp.messenger.core.security.FoundUserProfile
import com.hereapp.messenger.core.security.FounderConfig
import com.hereapp.messenger.core.security.OwnerGroupSummary
import com.hereapp.messenger.core.security.SecurityAdminRepository
import com.hereapp.messenger.core.security.UnprovisionedSecurityAdminRepository
import com.hereapp.messenger.core.security.VerificationRequestItem
import com.hereapp.messenger.ui.components.FounderOwnerBadge
import com.hereapp.messenger.ui.components.VerifiedBlueTick
import com.hereapp.messenger.ui.components.HerePrimaryButton
import com.hereapp.messenger.ui.components.HereSecondaryButton
import com.hereapp.messenger.ui.components.HereSectionHeader
import com.hereapp.messenger.ui.components.HereStatusBanner
import com.hereapp.messenger.ui.components.HereSurfaceCard
import com.hereapp.messenger.ui.components.HereTextField
import com.hereapp.messenger.ui.theme.HereAppAccent
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCenterScreen(
    currentLanguage: SupportedLanguage,
    selectedAccent: HereAppAccent,
    onBack: () -> Unit,
    adminRepository: SecurityAdminRepository = remember { UnprovisionedSecurityAdminRepository() }
) {
    val strings = LocalHereStrings.current
    val luxuryColors = LocalHereLuxuryColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val verificationRepo = remember(context) {
        FirebaseVerificationRepository(context.applicationContext)
    }

    var founderConfig by remember { mutableStateOf<FounderConfig?>(null) }
    var isOwner by remember { mutableStateOf(false) }
    var isLoadingAccess by remember { mutableStateOf(true) }

    var directHereAppId by remember { mutableStateOf("") }
    var searchedUser by remember { mutableStateOf<FoundUserProfile?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var pendingActionLabel by remember { mutableStateOf<String?>(null) }
    var pendingActionBlock by remember { mutableStateOf<(() -> Unit)?>(null) }

    var rejectTarget by remember { mutableStateOf<VerificationRequestItem?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var renameGroupTarget by remember { mutableStateOf<OwnerGroupSummary?>(null) }
    var renameGroupInput by remember { mutableStateOf("") }

    val pendingRequests by verificationRepo.observePendingRequests().collectAsState(initial = emptyList())
    val allGroups by verificationRepo.observeAllGroups().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        founderConfig = verificationRepo.getFounderConfig()
        isOwner = verificationRepo.isCurrentUserFounderOwner()
        isLoadingAccess = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Owner Panel",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("control_center_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                feedback?.let { HereStatusBanner(text = it) }

                when {
                    isLoadingAccess -> {
                        HereSurfaceCard {
                            Text(
                                text = "Verifying Founder & Owner credentials...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = luxuryColors.mutedText
                            )
                        }
                    }

                    founderConfig == null -> {
                        HereSectionHeader(title = "FOUNDER ONE-TIME LOCK")
                        HereSurfaceCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Bind Founder & Owner Account",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This one-time action permanently locks app_config/founder to your current Firebase UID, grants FOUNDER • OWNER + Blue Tick, and reserves ${IdentityConstants.FOUNDER_HEREAPP_ID}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = luxuryColors.mutedText
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            HerePrimaryButton(
                                text = "Lock Founder Account (${IdentityConstants.FOUNDER_HEREAPP_ID})",
                                onClick = {
                                    scope.launch {
                                        verificationRepo.bindFounderAccountOnce()
                                            .onSuccess { msg ->
                                                feedback = msg
                                                founderConfig = verificationRepo.getFounderConfig()
                                                isOwner = verificationRepo.isCurrentUserFounderOwner()
                                            }
                                            .onFailure { err ->
                                                feedback = err.message ?: "Failed to bind Founder account."
                                            }
                                    }
                                },
                                testTag = "control_center_bind_founder_button"
                            )
                        }
                    }

                    !isOwner -> {
                        HereSectionHeader(title = "ACCESS RESTRICTED")
                        HereSurfaceCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Owner Access Required",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This panel is restricted exclusively to the verified Founder & Owner account bound in Firestore (app_config/founder).",
                                style = MaterialTheme.typography.bodySmall,
                                color = luxuryColors.mutedText
                            )
                        }
                    }

                    else -> {
                        // 1. Founder & Owner Identity Status
                        HereSectionHeader(title = "FOUNDER & OWNER STATUS")
                        HereSurfaceCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Private Owner Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FounderOwnerBadge()
                            Spacer(modifier = Modifier.height(12.dp))
                            ControlMetricRow(
                                label = "Founder HereApp ID",
                                value = IdentityConstants.FOUNDER_HEREAPP_ID
                            )
                            HorizontalDivider(
                                color = luxuryColors.subtleDivider,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            ControlMetricRow(
                                label = "Backend Enforcement",
                                value = "firestore.rules (app_config/founder)"
                            )
                        }

                        // 2. Grant Verification (Search by HereApp ID or @Username)
                        HereSectionHeader(title = "GRANT VERIFICATION")
                        HereSurfaceCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Grant Verification",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Search by HereApp ID or @Username to grant or revoke the Verified badge. Founder & Owner privileges cannot be modified here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = luxuryColors.mutedText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HereTextField(
                                value = directHereAppId,
                                onValueChange = { directHereAppId = it },
                                label = "HereApp ID or @Username",
                                leadingIcon = Icons.Outlined.Verified,
                                testTag = "control_center_hereapp_id_input"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HerePrimaryButton(
                                text = "Search User",
                                onClick = {
                                    scope.launch {
                                        verificationRepo.findUserByUserId(directHereAppId)
                                            .onSuccess { found ->
                                                searchedUser = found
                                                if (found == null) {
                                                    feedback = "No user found for \"${directHereAppId.trim()}\"."
                                                } else {
                                                    feedback = null
                                                }
                                            }
                                            .onFailure { err ->
                                                searchedUser = null
                                                feedback = err.message
                                            }
                                    }
                                },
                                testTag = "control_center_search_user_id_button"
                            )

                            searchedUser?.let { found ->
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = luxuryColors.subtleDivider)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = found.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (found.isVerified) {
                                        VerifiedBlueTick(size = 18.dp)
                                    }
                                }
                                Text(
                                    text = "@${found.username} · ID: ${found.userId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = luxuryColors.mutedText
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HerePrimaryButton(
                                        text = "Grant Verified",
                                        onClick = {
                                            pendingActionLabel = "Confirm granting the Verified badge to @${found.username} (${found.userId})?"
                                            pendingActionBlock = {
                                                scope.launch {
                                                    verificationRepo.setVerifiedByUid(found.uid, found.userId, true)
                                                        .onSuccess { msg ->
                                                            feedback = msg
                                                            searchedUser = found.copy(isVerified = true)
                                                        }
                                                        .onFailure { err -> feedback = err.message }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "control_center_grant_verified_button"
                                    )
                                    HereSecondaryButton(
                                        text = "Revoke Verified",
                                        onClick = {
                                            pendingActionLabel = "Confirm revoking the Verified badge from @${found.username} (${found.userId})?"
                                            pendingActionBlock = {
                                                scope.launch {
                                                    verificationRepo.setVerifiedByUid(found.uid, found.userId, false)
                                                        .onSuccess { msg ->
                                                            feedback = msg
                                                            searchedUser = found.copy(isVerified = false)
                                                        }
                                                        .onFailure { err -> feedback = err.message }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "control_center_revoke_verified_button"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HereSecondaryButton(
                                    text = "Appoint Admin",
                                    onClick = {
                                        val id = directHereAppId.trim().uppercase()
                                        if (!IdentityConstants.isValidHrHereAppId(id)) {
                                            feedback = "Invalid format. Enter a valid HereApp ID like HR-0002."
                                            return@HereSecondaryButton
                                        }
                                        pendingActionLabel = "Appoint HereApp ID $id as App Admin?"
                                        pendingActionBlock = {
                                            scope.launch {
                                                verificationRepo.setAdminByHereAppId(id, isAdmin = true)
                                                    .onSuccess { msg -> feedback = msg }
                                                    .onFailure { err -> feedback = err.message }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "control_center_appoint_admin_button"
                                )
                                HereSecondaryButton(
                                    text = "Remove Admin",
                                    onClick = {
                                        val id = directHereAppId.trim().uppercase()
                                        if (!IdentityConstants.isValidHrHereAppId(id)) {
                                            feedback = "Invalid format. Enter a valid HereApp ID like HR-0002."
                                            return@HereSecondaryButton
                                        }
                                        pendingActionLabel = "Remove Admin privileges from HereApp ID $id?"
                                        pendingActionBlock = {
                                            scope.launch {
                                                verificationRepo.setAdminByHereAppId(id, isAdmin = false)
                                                    .onSuccess { msg -> feedback = msg }
                                                    .onFailure { err -> feedback = err.message }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "control_center_remove_admin_button"
                                )
                            }
                        }

                        // 3. Pending Verification Requests
                        HereSectionHeader(title = "طلبات التوثيق المعلّقة · VERIFICATION REQUESTS (${pendingRequests.size})")
                        if (pendingRequests.isEmpty()) {
                            HereSurfaceCard {
                                Text(
                                    text = "لا توجد طلبات توثيق معلّقة حالياً · No pending verification requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = luxuryColors.mutedText
                                )
                            }
                        } else {
                            pendingRequests.forEach { req ->
                                HereSurfaceCard {
                                    Text(
                                        text = req.displayName.ifBlank { "@${req.username}" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "@${req.username} · ID: ${req.hereAppId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (req.reason.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = req.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = luxuryColors.mutedText
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        HerePrimaryButton(
                                            text = "قبول · Approve",
                                            onClick = {
                                                pendingActionLabel = "Approve verification for ${req.hereAppId} (@${req.username})?"
                                                pendingActionBlock = {
                                                    scope.launch {
                                                        verificationRepo.approveRequest(req)
                                                            .onSuccess { msg -> feedback = msg }
                                                            .onFailure { err -> feedback = err.message }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            testTag = "control_center_approve_${req.uid}"
                                        )
                                        HereSecondaryButton(
                                            text = "رفض · Reject",
                                            onClick = {
                                                rejectReason = ""
                                                rejectTarget = req
                                            },
                                            modifier = Modifier.weight(1f),
                                            testTag = "control_center_reject_${req.uid}"
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Owner Group Management (Edit / Delete Any Group)
                        HereSectionHeader(title = "GROUP MANAGEMENT (${allGroups.size})")
                        if (allGroups.isEmpty()) {
                            HereSurfaceCard {
                                Text(
                                    text = "No groups found in Firestore.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = luxuryColors.mutedText
                                )
                            }
                        } else {
                            allGroups.forEach { group ->
                                HereSurfaceCard {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${group.memberCount} members · ${if (group.isPublic) "Public" else "Private"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = luxuryColors.mutedText
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        HereSecondaryButton(
                                            text = "Edit Name",
                                            onClick = {
                                                renameGroupInput = group.name
                                                renameGroupTarget = group
                                            },
                                            modifier = Modifier.weight(1f),
                                            testTag = "control_center_edit_group_${group.id}"
                                        )
                                        HereSecondaryButton(
                                            text = "Delete Group",
                                            onClick = {
                                                pendingActionLabel = "Permanently delete group \"${group.name}\"?"
                                                pendingActionBlock = {
                                                    scope.launch {
                                                        verificationRepo.deleteGroupAsOwner(group.id, group.name)
                                                            .onSuccess { msg -> feedback = msg }
                                                            .onFailure { err -> feedback = err.message }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            testTag = "control_center_delete_group_${group.id}"
                                        )
                                    }
                                }
                            }
                        }

                        // 5. System & Localization Diagnostics
                        HereSectionHeader(title = strings.sectionSystem)
                        HereSurfaceCard {
                            ControlMetricRow(
                                label = strings.languageLabel,
                                value = "${currentLanguage.nativeName} (${currentLanguage.code.uppercase()})"
                            )
                            HorizontalDivider(
                                color = luxuryColors.subtleDivider,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            ControlMetricRow(
                                label = strings.layoutDirectionLabel,
                                value = if (currentLanguage.isRtl) strings.rtlLabel else strings.ltrLabel
                            )
                            HorizontalDivider(
                                color = luxuryColors.subtleDivider,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            ControlMetricRow(
                                label = strings.accentThemeLabel,
                                value = selectedAccent.displayName
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingActionLabel != null) {
        AlertDialog(
            onDismissRequest = { pendingActionLabel = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Confirm Administrative Action", style = MaterialTheme.typography.titleLarge) },
            text = { Text(pendingActionLabel.orEmpty(), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingActionBlock?.invoke()
                        pendingActionLabel = null
                    },
                    modifier = Modifier.testTag("control_center_confirm_action")
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingActionLabel = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    rejectTarget?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("رفض طلب التوثيق · Reject Request", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "سيتم إرسال إشعار بالرفض إلى ${req.hereAppId} (@${req.username}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText
                    )
                    HereTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = "سبب الرفض (اختياري) / Rejection Reason",
                        singleLine = false,
                        testTag = "control_center_reject_reason_input"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            verificationRepo.rejectRequest(req, rejectReason)
                                .onSuccess { msg -> feedback = msg }
                                .onFailure { err -> feedback = err.message }
                        }
                        rejectTarget = null
                    },
                    modifier = Modifier.testTag("control_center_confirm_reject")
                ) {
                    Text("تأكيد الرفض", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    renameGroupTarget?.let { group ->
        AlertDialog(
            onDismissRequest = { renameGroupTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Group Name", style = MaterialTheme.typography.titleLarge) },
            text = {
                HereTextField(
                    value = renameGroupInput,
                    onValueChange = { renameGroupInput = it },
                    label = "New Group Name",
                    testTag = "control_center_rename_group_input"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            verificationRepo.renameGroupAsOwner(group.id, renameGroupInput)
                                .onSuccess { msg -> feedback = msg }
                                .onFailure { err -> feedback = err.message }
                        }
                        renameGroupTarget = null
                    },
                    modifier = Modifier.testTag("control_center_confirm_rename_group")
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameGroupTarget = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun ControlMetricRow(
    label: String,
    value: String
) {
    val luxuryColors = LocalHereLuxuryColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = luxuryColors.mutedText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
