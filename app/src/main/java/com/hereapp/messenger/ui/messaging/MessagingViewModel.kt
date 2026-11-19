package com.hereapp.messenger.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hereapp.messenger.core.messaging.ChatMessage
import com.hereapp.messenger.core.messaging.ConversationSummary
import com.hereapp.messenger.core.messaging.MessagingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessagingUiState(
    val isBackendConnected: Boolean = false,
    val conversations: List<ConversationSummary> = emptyList(),
    val showArchived: Boolean = false,
    val searchQuery: String = "",
    val activeConversation: ConversationSummary? = null,
    val messages: List<ChatMessage> = emptyList(),
    val hasMoreMessages: Boolean = false,
    val nextCursor: String? = null,
    val replyingTo: ChatMessage? = null,
    val editingMessage: ChatMessage? = null,
    val statusNotice: String? = null
)

class MessagingViewModel(
    private val repository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MessagingUiState(isBackendConnected = repository.isBackendConnected)
    )
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    init {
        observeConversations(includeArchived = false)
    }

    private fun observeConversations(includeArchived: Boolean) {
        viewModelScope.launch {
            repository.observeConversations(includeArchived).collect { list ->
                _uiState.update { it.copy(conversations = list) }
            }
        }
    }

    fun setShowArchived(showArchived: Boolean) {
        _uiState.update { it.copy(showArchived = showArchived) }
        observeConversations(includeArchived = showArchived)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openConversation(conversation: ConversationSummary) {
        _uiState.update {
            it.copy(
                activeConversation = conversation,
                replyingTo = null,
                editingMessage = null,
                statusNotice = null
            )
        }
        viewModelScope.launch {
            repository.observeMessages(conversation.id).collect { page ->
                _uiState.update {
                    it.copy(
                        messages = page.items,
                        hasMoreMessages = page.hasMore,
                        nextCursor = page.nextCursor
                    )
                }
            }
        }
    }

    fun startReply(message: ChatMessage) {
        _uiState.update { it.copy(replyingTo = message, editingMessage = null) }
    }

    fun startEdit(message: ChatMessage) {
        _uiState.update { it.copy(editingMessage = message, replyingTo = null) }
    }

    fun clearComposerContext() {
        _uiState.update { it.copy(replyingTo = null, editingMessage = null) }
    }

    fun sendOrEditMessage(conversationId: String, rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        val current = _uiState.value
        viewModelScope.launch {
            val result = if (current.editingMessage != null) {
                repository.editMessage(conversationId, current.editingMessage.id, text)
            } else {
                repository.sendMessage(conversationId, text, current.replyingTo?.id)
            }
            result.onSuccess {
                clearComposerContext()
            }.onFailure { err ->
                _uiState.update { it.copy(statusNotice = err.message) }
            }
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(conversationId, messageId)
                .onFailure { err -> _uiState.update { it.copy(statusNotice = err.message) } }
        }
    }

    fun toggleReaction(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(conversationId, messageId, emoji)
                .onFailure { err -> _uiState.update { it.copy(statusNotice = err.message) } }
        }
    }

    fun togglePin(conversationId: String, messageId: String, pin: Boolean) {
        viewModelScope.launch {
            repository.togglePinMessage(conversationId, messageId, pin)
                .onFailure { err -> _uiState.update { it.copy(statusNotice = err.message) } }
        }
    }

    fun setArchived(conversationId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.setConversationArchived(conversationId, archived)
                .onFailure { err -> _uiState.update { it.copy(statusNotice = err.message) } }
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(statusNotice = null) }
    }

    class Factory(
        private val repository: MessagingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessagingViewModel(repository) as T
        }
    }
}
