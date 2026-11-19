package com.hereapp.messenger.core.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

data class PresenceStatus(
    val isOnline: Boolean = false,
    val lastSeenEpochMs: Long? = null
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderUid: String,
    val text: String,
    val createdAtEpochMs: Long,
    val editedAtEpochMs: Long? = null,
    val deleted: Boolean = false,
    val replyToMessageId: String? = null,
    val replyPreviewText: String? = null,
    val reactions: Map<String, List<String>> = emptyMap(),
    val pinned: Boolean = false,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.SENT
)

data class ConversationSummary(
    val id: String,
    val participantUids: List<String>,
    val peerUid: String,
    val peerDisplayName: String,
    val peerUsername: String,
    val peerPresence: PresenceStatus = PresenceStatus(),
    val typingUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageAtEpochMs: Long = 0L,
    val unreadCount: Int = 0,
    val pinnedMessageId: String? = null,
    val archived: Boolean = false
)

data class MessagingPage<T>(
    val items: List<T> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

interface MessagingRepository {
    val isBackendConnected: Boolean
    fun observeConversations(includeArchived: Boolean = false): Flow<List<ConversationSummary>>
    fun observeMessages(conversationId: String, pageSize: Int = 30): Flow<MessagingPage<ChatMessage>>
    suspend fun loadOlderMessages(
        conversationId: String,
        beforeCursor: String,
        pageSize: Int = 30
    ): Result<MessagingPage<ChatMessage>>

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String? = null
    ): Result<ChatMessage>

    suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newText: String
    ): Result<Unit>

    suspend fun deleteMessage(
        conversationId: String,
        messageId: String
    ): Result<Unit>

    suspend fun toggleReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): Result<Unit>

    suspend fun togglePinMessage(
        conversationId: String,
        messageId: String,
        pin: Boolean
    ): Result<Unit>

    suspend fun setTyping(conversationId: String, isTyping: Boolean): Result<Unit>
    suspend fun markConversationRead(conversationId: String): Result<Unit>
    suspend fun setConversationArchived(conversationId: String, archived: Boolean): Result<Unit>
}

/**
 * Fail-closed repository used when cloud database (Firestore) is not yet provisioned.
 * Strictly NEVER injects fake messages or local-only substitutes.
 */
class UnprovisionedMessagingRepository : MessagingRepository {

    override val isBackendConnected: Boolean = false

    private fun unconfiguredFailure(): Result<Nothing> =
        Result.failure(
            IllegalStateException(
                "Cloud messaging backend (Firestore) is not provisioned. Enable Firebase to send, sync, and receive real-time messages."
            )
        )

    override fun observeConversations(includeArchived: Boolean): Flow<List<ConversationSummary>> =
        flowOf(emptyList())

    override fun observeMessages(conversationId: String, pageSize: Int): Flow<MessagingPage<ChatMessage>> =
        flowOf(MessagingPage())

    override suspend fun loadOlderMessages(
        conversationId: String,
        beforeCursor: String,
        pageSize: Int
    ): Result<MessagingPage<ChatMessage>> = unconfiguredFailure()

    override suspend fun sendMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String?
    ): Result<ChatMessage> = unconfiguredFailure()

    override suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newText: String
    ): Result<Unit> = unconfiguredFailure()

    override suspend fun deleteMessage(
        conversationId: String,
        messageId: String
    ): Result<Unit> = unconfiguredFailure()

    override suspend fun toggleReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): Result<Unit> = unconfiguredFailure()

    override suspend fun togglePinMessage(
        conversationId: String,
        messageId: String,
        pin: Boolean
    ): Result<Unit> = unconfiguredFailure()

    override suspend fun setTyping(conversationId: String, isTyping: Boolean): Result<Unit> =
        unconfiguredFailure()

    override suspend fun markConversationRead(conversationId: String): Result<Unit> =
        unconfiguredFailure()

    override suspend fun setConversationArchived(conversationId: String, archived: Boolean): Result<Unit> =
        unconfiguredFailure()
}
