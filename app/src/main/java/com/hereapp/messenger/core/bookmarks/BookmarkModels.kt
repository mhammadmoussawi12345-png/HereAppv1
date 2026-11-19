package com.hereapp.messenger.core.bookmarks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class BookmarkFolder(val label: String) {
    ALL("All"),
    IMPORTANT("Important"),
    WORK("Work"),
    PERSONAL("Personal")
}

data class BookmarkedMessage(
    val id: String,
    val conversationId: String,
    val conversationTitle: String,
    val messageId: String,
    val senderDisplayName: String,
    val text: String,
    val folder: BookmarkFolder = BookmarkFolder.IMPORTANT,
    val savedAtEpochMs: Long = 0L
)

interface BookmarkRepository {
    fun observeBookmarks(): Flow<List<BookmarkedMessage>>
    suspend fun saveBookmark(bookmark: BookmarkedMessage): Result<Unit>
    suspend fun removeBookmark(bookmarkId: String): Result<Unit>
    suspend fun updateFolder(bookmarkId: String, folder: BookmarkFolder): Result<Unit>
}

class UnprovisionedBookmarkRepository : BookmarkRepository {
    private fun unconfigured(): Result<Nothing> =
        Result.failure(
            IllegalStateException(
                "Cloud backend (Firestore) is not provisioned. Enable Firebase to save and sync Here Bookmarks across devices."
            )
        )

    override fun observeBookmarks(): Flow<List<BookmarkedMessage>> = flowOf(emptyList())
    override suspend fun saveBookmark(bookmark: BookmarkedMessage): Result<Unit> = unconfigured()
    override suspend fun removeBookmark(bookmarkId: String): Result<Unit> = unconfigured()
    override suspend fun updateFolder(bookmarkId: String, folder: BookmarkFolder): Result<Unit> = unconfigured()
}
