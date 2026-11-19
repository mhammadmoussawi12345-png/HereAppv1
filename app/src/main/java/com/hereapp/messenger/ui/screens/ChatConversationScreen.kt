package com.hereapp.messenger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hereapp.messenger.core.messaging.ChatMessage
import com.hereapp.messenger.core.messaging.ConversationSummary
import com.hereapp.messenger.core.messaging.MessageDeliveryStatus
import com.hereapp.messenger.ui.components.HereStatusBanner
import com.hereapp.messenger.ui.messaging.MessagingUiState
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatConversationScreen(
    uiState: MessagingUiState,
    currentUserUid: String,
    onBack: () -> Unit,
    onSendOrEdit: (conversationId: String, text: String) -> Unit,
    onStartReply: (ChatMessage) -> Unit,
    onStartEdit: (ChatMessage) -> Unit,
    onClearComposerContext: () -> Unit,
    onDeleteMessage: (conversationId: String, messageId: String) -> Unit,
    onToggleReaction: (conversationId: String, messageId: String, emoji: String) -> Unit,
    onTogglePin: (conversationId: String, messageId: String, pin: Boolean) -> Unit,
    onArchiveConversation: (conversationId: String, archived: Boolean) -> Unit
) {
    val conversation = uiState.activeConversation
    val luxuryColors = LocalHereLuxuryColors.current
    var draftText by remember(uiState.editingMessage?.id) {
        mutableStateOf(uiState.editingMessage?.text.orEmpty())
    }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    var showPeerProfileDialog by remember { mutableStateOf(false) }

    val pinnedMessage = remember(uiState.messages, conversation?.pinnedMessageId) {
        uiState.messages.firstOrNull { it.pinned || it.id == conversation?.pinnedMessageId }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = conversation != null) { showPeerProfileDialog = true }
                            .padding(vertical = 4.dp)
                            .testTag("chat_peer_profile_trigger"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = conversation?.peerDisplayName ?: "Here Chat",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = when {
                                conversation == null -> "Messaging"
                                conversation.typingUids.any { it != currentUserUid } -> "typing..."
                                conversation.peerPresence.isOnline -> "Online"
                                else -> "Offline"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (subtitle == "Online" || subtitle == "typing...") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    luxuryColors.mutedText
                                }
                            )
                        }
                    }
                },
                actions = {
                    // Messaging-focused only: NO voice or video call icons.
                    if (conversation != null) {
                        IconButton(
                            onClick = { onArchiveConversation(conversation.id, !conversation.archived) },
                            modifier = Modifier.testTag("chat_archive_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = if (conversation.archived) "Unarchive" else "Archive",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            ) {
                HorizontalDivider(color = luxuryColors.subtleDivider)

                // Reply or Edit Context Banner
                val contextLabel = when {
                    uiState.editingMessage != null -> "Editing message"
                    uiState.replyingTo != null -> "Replying to: ${uiState.replyingTo.text}"
                    else -> null
                }
                if (contextLabel != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = contextLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearComposerContext,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Minimal Luxury Message Composer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        placeholder = {
                            Text(
                                text = "Message...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = luxuryColors.mutedText
                            )
                        },
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = luxuryColors.hairlineBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )
                    IconButton(
                        onClick = {
                            val convId = conversation?.id ?: return@IconButton
                            onSendOrEdit(convId, draftText)
                            draftText = ""
                        },
                        enabled = draftText.isNotBlank() && conversation != null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (draftText.isNotBlank() && conversation != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (draftText.isNotBlank() && conversation != null) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (pinnedMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, luxuryColors.subtleDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = pinnedMessage.text,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            uiState.statusNotice?.let { notice ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HereStatusBanner(text = notice)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    val isMine = msg.senderUid == currentUserUid
                    MessageBubble(
                        message = msg,
                        isMine = isMine,
                        onLongClick = { selectedMessageForActions = msg }
                    )
                }
            }
        }
    }

    // Message Action Sheet (Reply, React, Pin, Edit, Delete)
    val actionMsg = selectedMessageForActions
    if (actionMsg != null && conversation != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForActions = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick reactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("👍", "❤️", "✨", "🔥", "🙏").forEach { emoji ->
                        TextButton(
                            onClick = {
                                onToggleReaction(conversation.id, actionMsg.id, emoji)
                                selectedMessageForActions = null
                            }
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                HorizontalDivider(color = luxuryColors.subtleDivider)
                ActionRow(icon = Icons.Outlined.Reply, label = "Reply") {
                    onStartReply(actionMsg)
                    selectedMessageForActions = null
                }
                ActionRow(
                    icon = Icons.Outlined.PushPin,
                    label = if (actionMsg.pinned) "Unpin message" else "Pin message"
                ) {
                    onTogglePin(conversation.id, actionMsg.id, !actionMsg.pinned)
                    selectedMessageForActions = null
                }
                if (actionMsg.senderUid == currentUserUid && !actionMsg.deleted) {
                    ActionRow(icon = Icons.Outlined.Edit, label = "Edit message") {
                        onStartEdit(actionMsg)
                        selectedMessageForActions = null
                    }
                    ActionRow(icon = Icons.Outlined.DeleteOutline, label = "Delete message") {
                        onDeleteMessage(conversation.id, actionMsg.id)
                        selectedMessageForActions = null
                    }
                }
            }
        }
    }

    if (showPeerProfileDialog && conversation != null) {
        AlertDialog(
            onDismissRequest = { showPeerProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(conversation.peerDisplayName, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "@${conversation.peerUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (conversation.peerPresence.isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPeerProfileDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    onLongClick: () -> Unit
) {
    val luxuryColors = LocalHereLuxuryColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color = if (isMine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (isMine) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                } else {
                    luxuryColors.hairlineBorder
                }
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!message.replyPreviewText.isNullOrBlank()) {
                    Text(
                        text = message.replyPreviewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = if (message.deleted) "Message deleted" else message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.deleted) luxuryColors.mutedText else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (message.editedAtEpochMs != null && !message.deleted) {
                        Text(
                            text = "edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = luxuryColors.mutedText
                        )
                    }
                    if (isMine) {
                        val statusIcon = when (message.status) {
                            MessageDeliveryStatus.SENDING -> Icons.Outlined.Schedule
                            MessageDeliveryStatus.SENT -> Icons.Filled.Done
                            MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.READ -> Icons.Filled.DoneAll
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = message.status.name,
                            tint = if (message.status == MessageDeliveryStatus.READ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                luxuryColors.mutedText
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
