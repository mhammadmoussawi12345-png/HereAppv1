package com.hereapp.messenger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.Send
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.hereapp.messenger.core.i18n.LocalHereStrings
import com.hereapp.messenger.ui.components.HereEmptyState
import com.hereapp.messenger.ui.components.HereSecondaryButton
import com.hereapp.messenger.ui.components.HereSurfaceCard
import com.hereapp.messenger.ui.components.HereTextField
import com.hereapp.messenger.ui.theme.LocalHereLuxuryColors

private data class WhatsAppChatItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int,
    val isPinned: Boolean,
    val updatedAtMillis: Long
)

private data class SelectableContact(
    val uid: String,
    val displayName: String,
    val username: String,
    val hereAppId: String,
    val isVerified: Boolean
)

@Composable
fun ChatsScreen(
    showArchived: Boolean = false,
    onToggleArchived: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalHereStrings.current
    val luxuryColors = LocalHereLuxuryColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val auth = remember { Firebase.auth }
    val db = remember(context) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance(
            context.getString(com.hereapp.messenger.R.string.firestore_database_id)
        )
    }
    val currentUser = auth.currentUser

    var searchQuery by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<WhatsAppChatItem?>(null) }
    var threadDraft by remember { mutableStateOf("") }
    var isSendingThreadMsg by remember { mutableStateOf(false) }
    var showSelectContact by remember { mutableStateOf(false) }
    var contactSearchQuery by remember { mutableStateOf("") }
    var showNewContactDialog by remember { mutableStateOf(false) }
    var newChatRecipient by remember { mutableStateOf("") }
    var newChatMessage by remember { mutableStateOf("") }
    var chatFeedback by remember { mutableStateOf<String?>(null) }
    var isCreatingChat by remember { mutableStateOf(false) }

    val chatsFlow = remember(db, currentUser?.uid) {
        if (currentUser == null) {
            kotlinx.coroutines.flow.flowOf(emptyList<WhatsAppChatItem>())
        } else {
            db.collection("chats")
                .whereArrayContains("participantUids", currentUser.uid)
                .snapshots()
                .map { snap ->
                    val timeFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                    snap.documents.map { doc ->
                        val ts = doc.getTimestamp("updatedAt")?.toDate()
                        WhatsAppChatItem(
                            id = doc.id,
                            title = doc.getString("peerName")?.ifBlank { null }
                                ?: doc.getString("title").orEmpty().ifBlank { "HereApp Chat" },
                            lastMessage = doc.getString("lastMessage").orEmpty().ifBlank { "Tap to start chatting" },
                            timeLabel = if (ts != null) timeFmt.format(ts) else "Now",
                            unreadCount = (doc.getLong("unreadCount") ?: 0L).toInt(),
                            isPinned = doc.getBoolean("isPinned") == true,
                            updatedAtMillis = ts?.time ?: 0L
                        )
                    }.sortedWith(
                        compareByDescending<WhatsAppChatItem> { it.isPinned }
                            .thenByDescending { it.updatedAtMillis }
                    )
                }
                .catch { err ->
                    if (err is kotlinx.coroutines.CancellationException) throw err
                    emit(emptyList())
                }
        }
    }

    val contactsFlow = remember(db, currentUser?.uid, showSelectContact) {
        if (currentUser == null || !showSelectContact) {
            kotlinx.coroutines.flow.flowOf(emptyList<SelectableContact>())
        } else {
            db.collection("users")
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        if (doc.id == currentUser.uid) return@mapNotNull null
                        SelectableContact(
                            uid = doc.id,
                            displayName = doc.getString("displayName").orEmpty().ifBlank { "HereApp User" },
                            username = doc.getString("username").orEmpty(),
                            hereAppId = doc.getString("userId") ?: doc.getString("hereAppId").orEmpty(),
                            isVerified = doc.getBoolean("isVerified") == true
                        )
                    }
                }
                .catch { err ->
                    if (err is kotlinx.coroutines.CancellationException) throw err
                    emit(emptyList())
                }
        }
    }

    val chats by chatsFlow.collectAsState(initial = emptyList())
    val contacts by contactsFlow.collectAsState(initial = emptyList())

    val filteredChats = remember(chats, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) chats
        else chats.filter {
            it.title.contains(q, ignoreCase = true) || it.lastMessage.contains(q, ignoreCase = true)
        }
    }

    val filteredContacts = remember(contacts, contactSearchQuery) {
        val q = contactSearchQuery.trim()
        if (q.isEmpty()) contacts
        else contacts.filter {
            it.displayName.contains(q, ignoreCase = true) ||
                it.username.contains(q, ignoreCase = true) ||
                it.hereAppId.contains(q, ignoreCase = true)
        }
    }

    if (selectedChat != null) {
        val activeChat = selectedChat!!
        androidx.activity.compose.BackHandler { selectedChat = null }

        val threadFlow = remember(db, activeChat.id) {
            db.collection("chats").document(activeChat.id).collection("messages")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limitToLast(100)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: return@mapNotNull null
                        Triple(
                            doc.id,
                            doc.getString("senderUid").orEmpty(),
                            text
                        )
                    }
                }
                .catch { err ->
                    if (err is kotlinx.coroutines.CancellationException) throw err
                    emit(emptyList())
                }
        }
        val threadMessages by threadFlow.collectAsState(initial = emptyList())

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.IconButton(
                    onClick = { selectedChat = null },
                    modifier = Modifier.testTag("chat_thread_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = activeChat.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = threadMessages.size,
                    key = { threadMessages[it].first }
                ) { idx ->
                    val (_, senderUid, text) = threadMessages[idx]
                    val isMine = senderUid == currentUser?.uid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = if (isMine) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    HereTextField(
                        value = threadDraft,
                        onValueChange = { threadDraft = it },
                        label = "Message...",
                        testTag = "chat_thread_input"
                    )
                }
                androidx.compose.material3.FilledIconButton(
                    enabled = !isSendingThreadMsg && threadDraft.isNotBlank() && currentUser != null,
                    onClick = {
                        val user = currentUser ?: return@FilledIconButton
                        val msg = threadDraft.trim()
                        if (msg.isEmpty()) return@FilledIconButton
                        isSendingThreadMsg = true
                        scope.launch {
                            try {
                                db.collection("chats").document(activeChat.id)
                                    .collection("messages")
                                    .add(
                                        mapOf(
                                            "text" to msg,
                                            "senderUid" to user.uid,
                                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                        )
                                    ).await()
                                db.collection("chats").document(activeChat.id)
                                    .update(
                                        mapOf(
                                            "lastMessage" to msg,
                                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                        )
                                    ).await()
                                threadDraft = ""
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                chatFeedback = e.localizedMessage
                            } finally {
                                isSendingThreadMsg = false
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_thread_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    } else if (showSelectContact) {
        androidx.activity.compose.BackHandler { showSelectContact = false }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.IconButton(
                    onClick = { showSelectContact = false },
                    modifier = Modifier.testTag("select_contact_back_button")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "Select Contact",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${filteredContacts.size} contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HereTextField(
                value = contactSearchQuery,
                onValueChange = { contactSearchQuery = it },
                label = "Search name, @username, or ID",
                leadingIcon = Icons.Outlined.Search,
                testTag = "select_contact_search_input"
            )
            Spacer(modifier = Modifier.height(14.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Quick Action 1: New Group
                item(key = "action_new_group") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .testTag("select_contact_new_group"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = "New Group",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "New Group",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Quick Action 2: New Contact
                item(key = "action_new_contact") {
                    androidx.compose.material3.Surface(
                        onClick = { showNewContactDialog = true },
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier.testTag("select_contact_new_contact")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonOutline,
                                    contentDescription = "New Contact",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = "New Contact",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item(key = "contacts_header") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "CONTACTS ON HEREAPP",
                        style = MaterialTheme.typography.labelMedium,
                        color = luxuryColors.mutedText,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                if (filteredContacts.isEmpty()) {
                    item(key = "contacts_empty") {
                        Text(
                            text = "No other users found yet. Tap New Contact above to start a chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = luxuryColors.mutedText,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(
                        count = filteredContacts.size,
                        key = { filteredContacts[it].uid }
                    ) { idx ->
                        val contact = filteredContacts[idx]
                        androidx.compose.material3.Surface(
                            onClick = {
                                val user = currentUser ?: return@Surface
                                scope.launch {
                                    try {
                                        db.collection("chats").add(
                                            mapOf(
                                                "peerName" to contact.displayName,
                                                "peerUid" to contact.uid,
                                                "lastMessage" to "Started a new conversation",
                                                "participantUids" to listOf(user.uid, contact.uid),
                                                "unreadCount" to 0,
                                                "isPinned" to false,
                                                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                            )
                                        ).await()
                                        showSelectContact = false
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        chatFeedback = e.localizedMessage
                                        showSelectContact = false
                                    }
                                }
                            },
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            modifier = Modifier.testTag("select_contact_user_${contact.uid}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = contact.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (contact.isVerified) {
                                            com.hereapp.messenger.ui.components.VerifiedBlueTick(size = 16.dp)
                                        }
                                    }
                                    Text(
                                        text = "@${contact.username}${contact.hereAppId.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = luxuryColors.mutedText
                                    )
                                }
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(color = luxuryColors.subtleDivider)
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Bar: Title "Chats" on the left
                Text(
                    text = strings.navChats,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Search bar below title
                HereTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = strings.searchPlaceholder,
                    leadingIcon = Icons.Outlined.Search,
                    testTag = "chats_search_input"
                )
                Spacer(modifier = Modifier.height(12.dp))

                chatFeedback?.let { msg ->
                    com.hereapp.messenger.ui.components.HereStatusBanner(text = msg)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (filteredChats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HereEmptyState(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = "No chats yet",
                            body = if (currentUser == null) {
                                "Sign in to load your conversations from Firestore."
                            } else {
                                "Tap the + button below to select a contact and start chatting."
                            }
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(
                            count = filteredChats.size,
                            key = { filteredChats[it].id }
                        ) { index ->
                            val chat = filteredChats[index]
                            androidx.compose.material3.Surface(
                                onClick = { selectedChat = chat },
                                color = androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.testTag("chat_row_${chat.id}")
                            ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.title.trim().take(1).uppercase().ifBlank { "H" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chat.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = chat.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = luxuryColors.mutedText,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = chat.timeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else luxuryColors.mutedText
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (chat.isPinned) {
                                            Icon(
                                                imageVector = Icons.Outlined.PushPin,
                                                contentDescription = "Pinned chat",
                                                tint = luxuryColors.mutedText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (chat.unreadCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            }
                            androidx.compose.material3.HorizontalDivider(color = luxuryColors.subtleDivider)
                        }
                    }
                }
            }

            // Gold "+" Floating Action Button (HereApp Black & Gold theme — no green)
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    if (currentUser == null) {
                        chatFeedback = "Please sign in first to select a contact."
                    } else {
                        showSelectContact = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("chats_new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Select contact"
                )
            }
        }
    }

    if (showNewContactDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!isCreatingChat) showNewContactDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("New Contact", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HereTextField(
                        value = newChatRecipient,
                        onValueChange = { newChatRecipient = it },
                        label = "Contact @username or Name",
                        testTag = "chats_new_recipient_input"
                    )
                    HereTextField(
                        value = newChatMessage,
                        onValueChange = { newChatMessage = it },
                        label = "First message",
                        testTag = "chats_new_message_input"
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = !isCreatingChat && newChatRecipient.isNotBlank(),
                    onClick = {
                        val user = currentUser ?: return@TextButton
                        val peer = newChatRecipient.trim()
                        val firstMsg = newChatMessage.trim().ifBlank { "Hello!" }
                        isCreatingChat = true
                        scope.launch {
                            try {
                                db.collection("chats").add(
                                    mapOf(
                                        "peerName" to peer,
                                        "lastMessage" to firstMsg,
                                        "participantUids" to listOf(user.uid),
                                        "unreadCount" to 0,
                                        "isPinned" to false,
                                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                ).await()
                                newChatRecipient = ""
                                newChatMessage = ""
                                showNewContactDialog = false
                                showSelectContact = false
                                chatFeedback = null
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                chatFeedback = e.localizedMessage ?: "Unable to create chat."
                                showNewContactDialog = false
                            } finally {
                                isCreatingChat = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("chats_confirm_new_chat_button")
                ) {
                    Text("Start Chat", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    enabled = !isCreatingChat,
                    onClick = { showNewContactDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun GroupsScreen(
    groupsRepository: com.hereapp.messenger.core.groups.GroupsRepository? = null,
    currentUserUid: String = "",
    onCreateGroupClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalHereStrings.current
    val luxuryColors = LocalHereLuxuryColors.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showPublicDiscovery by remember { mutableStateOf(false) }

    val groupsFlow = remember(groupsRepository, showPublicDiscovery, searchQuery) {
        if (groupsRepository == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else if (showPublicDiscovery) {
            groupsRepository.observePublicGroups(searchQuery)
        } else {
            groupsRepository.observeMyGroups()
        }
    }
    val groups by groupsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.navGroups,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            androidx.compose.material3.TextButton(
                onClick = onCreateGroupClick,
                modifier = Modifier.testTag("groups_create_button")
            ) {
                Text("Create Group", color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HereTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = strings.searchPlaceholder,
            leadingIcon = Icons.Outlined.Search,
            testTag = "groups_search_input"
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.FilterChip(
                selected = !showPublicDiscovery,
                onClick = { showPublicDiscovery = false },
                label = { Text("My Groups") },
                modifier = Modifier.testTag("groups_filter_mine")
            )
            androidx.compose.material3.FilterChip(
                selected = showPublicDiscovery,
                onClick = { showPublicDiscovery = true },
                label = { Text("Public Discovery") },
                modifier = Modifier.testTag("groups_filter_public")
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (groups.isEmpty()) {
            HereEmptyState(
                icon = Icons.Outlined.Groups,
                title = if (showPublicDiscovery) "Public Group Directory Ready" else strings.groupsEmptyTitle,
                body = strings.groupsEmptyBody
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                groups.forEach { group ->
                    val isMember = group.members.any { it.uid == currentUserUid }
                    HereSurfaceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (group.description.isNotBlank()) {
                                    Text(
                                        text = group.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = luxuryColors.mutedText
                                    )
                                }
                                Text(
                                    text = "${group.members.size} member(s) · ${if (group.isPublic) "Public" else "Private"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (groupsRepository != null && currentUserUid.isNotBlank()) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        scope.launch {
                                            if (isMember) {
                                                groupsRepository.leaveGroup(group.id)
                                            } else {
                                                groupsRepository.joinGroup(group.id)
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isMember) "Leave" else "Join",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    val strings = LocalHereStrings.current
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = strings.discoverTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(14.dp))
        HereTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = strings.searchPlaceholder,
            leadingIcon = Icons.Outlined.Search,
            testTag = "discover_search_input"
        )
        Spacer(modifier = Modifier.height(24.dp))
        HereEmptyState(
            icon = Icons.Outlined.Explore,
            title = strings.discoverEmptyTitle,
            body = strings.discoverEmptyBody
        )
    }
}

@Composable
fun ProfileScreen(
    authSession: com.hereapp.messenger.core.auth.AuthSession? = null,
    onOpenProfileSetup: () -> Unit,
    onOpenMyQr: () -> Unit = {},
    onOpenScanQr: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenControlCenter: () -> Unit,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalHereStrings.current
    val luxuryColors = LocalHereLuxuryColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val verificationRepo = remember(context) {
        com.hereapp.messenger.core.security.FirebaseVerificationRepository(context.applicationContext)
    }
    val profile = authSession?.profile

    var isFounderOwner by remember { mutableStateOf(false) }
    var founderConfigured by remember { mutableStateOf(true) }
    var showVerifyRequestDialog by remember { mutableStateOf(false) }
    var verifyReason by remember { mutableStateOf("") }
    var verifyFeedback by remember { mutableStateOf<String?>(null) }
    var isSubmittingVerify by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(authSession?.firebaseUid) {
        if (authSession != null) {
            val founderCfg = verificationRepo.getFounderConfig()
            founderConfigured = founderCfg != null
            isFounderOwner = founderCfg != null && founderCfg.ownerUid == authSession.firebaseUid
        } else {
            isFounderOwner = false
        }
    }

    val showFounderBadge = isFounderOwner ||
        profile?.isOwner == true ||
        profile?.hereAppId == com.hereapp.messenger.core.identity.IdentityConstants.FOUNDER_HEREAPP_ID
    val showVerifiedTick = showFounderBadge || profile?.isVerified == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = strings.profileTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Default Black & Gold Profile Banner
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.hereapp.messenger.R.drawable.img_welcome_hero_1795082510566),
                contentDescription = "Profile Banner",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        verifyFeedback?.let { msg ->
            com.hereapp.messenger.ui.components.HereStatusBanner(text = msg)
        }

        HereSurfaceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = strings.profileTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile?.displayName?.ifBlank { null }
                                ?: authSession?.email
                                ?: strings.profileGuestTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showVerifiedTick) {
                            com.hereapp.messenger.ui.components.VerifiedBlueTick(size = 18.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            profile != null && profile.username.isNotBlank() ->
                                "@${profile.username}${profile.hereAppId?.let { " · ID $it" }.orEmpty()}"
                            authSession != null ->
                                if (authSession.emailVerified) "Email verified" else "Email verification pending"
                            else -> strings.profileGuestBody
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText
                    )
                    if (showFounderBadge) {
                        Spacer(modifier = Modifier.height(6.dp))
                        com.hereapp.messenger.ui.components.FounderOwnerBadge()
                    }
                    if (authSession != null) {
                        var isEmailVerified by remember(authSession.firebaseUid, authSession.emailVerified) {
                            mutableStateOf(Firebase.auth.currentUser?.isEmailVerified ?: authSession.emailVerified)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.testTag("profile_email_verification_status")
                        ) {
                            Text(
                                text = if (isEmailVerified) "✓ Email Verified" else "⚠ Email Not Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isEmailVerified) luxuryColors.success else luxuryColors.warning
                            )
                            if (!isEmailVerified) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                Firebase.auth.currentUser?.sendEmailVerification()?.await()
                                                verifyFeedback = "Verification email sent to ${authSession.email}."
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                verifyFeedback = e.localizedMessage ?: "Unable to send verification email."
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("profile_resend_email_verification_button")
                                ) {
                                    Text("Resend", style = MaterialTheme.typography.labelSmall)
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                Firebase.auth.currentUser?.reload()?.await()
                                                isEmailVerified = Firebase.auth.currentUser?.isEmailVerified == true
                                                verifyFeedback = if (isEmailVerified) {
                                                    "Email verified!"
                                                } else {
                                                    "Email is still pending verification."
                                                }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                verifyFeedback = e.localizedMessage ?: "Unable to refresh status."
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("profile_refresh_email_verification_button")
                                ) {
                                    Text("Refresh", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HereSecondaryButton(
                    text = "My QR Code",
                    onClick = onOpenMyQr,
                    modifier = Modifier.weight(1f),
                    testTag = "profile_my_qr_button"
                )
                HereSecondaryButton(
                    text = "Scan QR",
                    onClick = onOpenScanQr,
                    modifier = Modifier.weight(1f),
                    testTag = "profile_scan_qr_button"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HereSecondaryButton(
                text = strings.openProfileSetup,
                onClick = onOpenProfileSetup,
                testTag = "profile_open_setup_button"
            )
            if (authSession != null && !showVerifiedTick) {
                Spacer(modifier = Modifier.height(10.dp))
                HereSecondaryButton(
                    text = "طلب توثيق الحساب · Request Verification",
                    onClick = { showVerifyRequestDialog = true },
                    testTag = "profile_request_verification_button"
                )
            }
            if (authSession != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HereSecondaryButton(
                    text = "Sign Out",
                    onClick = onSignOut,
                    testTag = "profile_sign_out_button"
                )
            }
        }

        HereSurfaceCard(
            onClick = onOpenBookmarks,
            modifier = Modifier.testTag("profile_bookmarks_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Here Bookmarks",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Here Bookmarks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saved messages, folders & search",
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Owner Panel is visible exclusively to the bound Founder/Owner (or during one-time initial Founder setup)
        if (isFounderOwner || !founderConfigured) {
            HereSurfaceCard(
                onClick = onOpenControlCenter,
                modifier = Modifier.testTag("profile_control_center_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = strings.openControlCenter,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFounderOwner) "Owner Panel · ${strings.openControlCenter}" else "Bind Founder Account (HR-0001)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFounderOwner) "Verification requests & HereApp ID management" else "One-time Founder & Owner lock setup",
                            style = MaterialTheme.typography.bodySmall,
                            color = luxuryColors.mutedText
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showVerifyRequestDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!isSubmittingVerify) showVerifyRequestDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "طلب توثيق الحساب",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "سيتم مراجعة طلبك من قِبل الإدارة باستخدام معرّف HereApp ID الخاص بك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = luxuryColors.mutedText
                    )
                    com.hereapp.messenger.ui.components.HereTextField(
                        value = verifyReason,
                        onValueChange = { verifyReason = it },
                        label = "سبب طلب التوثيق / Verification Reason",
                        singleLine = false,
                        testTag = "profile_verify_reason_input"
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = !isSubmittingVerify,
                    onClick = {
                        val currentProfile = profile
                        if (currentProfile == null || currentProfile.hereAppId.isNullOrBlank()) {
                            verifyFeedback = "Please complete Profile Setup first to get your HereApp ID."
                            showVerifyRequestDialog = false
                            return@TextButton
                        }
                        isSubmittingVerify = true
                        scope.launch {
                            verificationRepo.submitVerificationRequest(
                                hereAppId = currentProfile.hereAppId,
                                username = currentProfile.username,
                                displayName = currentProfile.displayName,
                                reason = verifyReason
                            ).onSuccess { msg ->
                                verifyFeedback = msg
                                verifyReason = ""
                                showVerifyRequestDialog = false
                            }.onFailure { err ->
                                verifyFeedback = err.message ?: "Failed to submit verification request."
                                showVerifyRequestDialog = false
                            }
                            isSubmittingVerify = false
                        }
                    },
                    modifier = Modifier.testTag("profile_verify_submit_button")
                ) {
                    Text("إرسال الطلب", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    enabled = !isSubmittingVerify,
                    onClick = { showVerifyRequestDialog = false }
                ) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
