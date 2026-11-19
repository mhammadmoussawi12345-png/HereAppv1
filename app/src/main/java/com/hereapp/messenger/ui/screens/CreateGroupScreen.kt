package com.hereapp.messenger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hereapp.messenger.core.groups.GroupPermissions
import com.hereapp.messenger.core.groups.GroupsRepository
import com.hereapp.messenger.ui.components.HerePrimaryButton
import com.hereapp.messenger.ui.components.HereSectionHeader
import com.hereapp.messenger.ui.components.HereStatusBanner
import com.hereapp.messenger.ui.components.HereSurfaceCard
import com.hereapp.messenger.ui.components.HereTextField
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    groupsRepository: GroupsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val luxuryColors = LocalHereLuxuryColors.current

    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isPublic by remember { mutableStateOf(false) }
    var membersCanSend by remember { mutableStateOf(true) }
    var membersCanAdd by remember { mutableStateOf(false) }
    var modsCanPin by remember { mutableStateOf(true) }
    var modsCanRemove by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Create Group", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("create_group_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                feedback?.let { HereStatusBanner(text = it) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(16.dp)
                        .testTag("create_group_photo_picker"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddAPhoto,
                            contentDescription = "Group image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text("Group Profile Image", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (photoUri != null) "Image selected" else "Tap to choose photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = luxuryColors.mutedText
                        )
                    }
                }

                HereTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = "Group Name",
                    leadingIcon = Icons.Outlined.Groups,
                    testTag = "create_group_name_input"
                )

                HereTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Group Description",
                    singleLine = false,
                    testTag = "create_group_description_input"
                )

                HereSectionHeader(title = "VISIBILITY & PERMISSIONS")
                HereSurfaceCard {
                    PermissionToggleRow(
                        label = "Public Group Discovery",
                        checked = isPublic,
                        onCheckedChange = { isPublic = it }
                    )
                    PermissionToggleRow(
                        label = "Members can send messages",
                        checked = membersCanSend,
                        onCheckedChange = { membersCanSend = it }
                    )
                    PermissionToggleRow(
                        label = "Members can add others",
                        checked = membersCanAdd,
                        onCheckedChange = { membersCanAdd = it }
                    )
                    PermissionToggleRow(
                        label = "Moderators can pin messages",
                        checked = modsCanPin,
                        onCheckedChange = { modsCanPin = it }
                    )
                    PermissionToggleRow(
                        label = "Moderators can remove members",
                        checked = modsCanRemove,
                        onCheckedChange = { modsCanRemove = it }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                HerePrimaryButton(
                    text = "Create Group",
                    onClick = {
                        if (groupName.trim().length < 2) {
                            feedback = "Please enter a group name (at least 2 characters)."
                            return@HerePrimaryButton
                        }
                        scope.launch {
                            groupsRepository.createGroup(
                                name = groupName.trim(),
                                description = description.trim(),
                                photoUri = photoUri?.toString(),
                                isPublic = isPublic,
                                permissions = GroupPermissions(
                                    membersCanSendMessages = membersCanSend,
                                    membersCanAddOthers = membersCanAdd,
                                    moderatorsCanPinMessages = modsCanPin,
                                    moderatorsCanRemoveMembers = modsCanRemove
                                )
                            ).onSuccess {
                                onBack()
                            }.onFailure { err ->
                                feedback = err.message
                            }
                        }
                    },
                    testTag = "create_group_submit_button"
                )
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
