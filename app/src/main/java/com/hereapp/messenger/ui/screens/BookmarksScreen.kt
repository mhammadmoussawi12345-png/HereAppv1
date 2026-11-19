package com.hereapp.messenger.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hereapp.messenger.core.bookmarks.BookmarkFolder
import com.hereapp.messenger.core.bookmarks.BookmarkRepository
import com.hereapp.messenger.ui.components.HereEmptyState
import com.hereapp.messenger.ui.components.HereTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarkRepository: BookmarkRepository,
    onBack: () -> Unit
) {
    val bookmarks by bookmarkRepository.observeBookmarks().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf(BookmarkFolder.ALL) }

    val filtered = remember(bookmarks, searchQuery, selectedFolder) {
        bookmarks.filter { item ->
            val matchesFolder = selectedFolder == BookmarkFolder.ALL || item.folder == selectedFolder
            val matchesQuery = searchQuery.isBlank() ||
                item.text.contains(searchQuery, ignoreCase = true) ||
                item.conversationTitle.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("Here Bookmarks", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("bookmarks_back_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            HereTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search saved messages...",
                leadingIcon = Icons.Outlined.Search,
                testTag = "bookmarks_search_input"
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BookmarkFolder.entries) { folder ->
                    FilterChip(
                        selected = selectedFolder == folder,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder.label) },
                        modifier = Modifier.testTag("bookmark_folder_${folder.name.lowercase()}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (filtered.isEmpty()) {
                HereEmptyState(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = "No Saved Messages Yet",
                    body = "Save important messages from any chat or group to organize and sync them across devices once cloud storage is provisioned."
                )
            }
        }
    }
}
