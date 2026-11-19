package com.hereapp.messenger.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object HereEmojiCatalog {
    val frequent = listOf("✨", "❤️", "👍", "🙏", "🔥", "😊", "👏", "🤍", "🎉", "🥂")
    val smileys = listOf(
        "😀", "😃", "😄", "😁", "😊", "🙂", "😉", "😌", "😍", "🥰",
        "😘", "😎", "🤩", "🤔", "🤗", "🤭", "🤫", "🫡", "😴", "🥹"
    )
    val gestures = listOf(
        "👍", "👎", "👏", "🙌", "🤝", "🙏", "✌️", "🤞", "🫶", "👌",
        "👋", "🫡", "💪", "☝️", "✋", "🤚"
    )
    val hearts = listOf(
        "❤️", "🤍", "🖤", "🤎", "💛", "💙", "💜", "💚", "🧡", "✨",
        "💫", "⭐", "🌟", "🔥", "💎", "🕊️"
    )
    val objects = listOf(
        "☕", "🍵", "🥂", "🎵", "📸", "📍", "✈️", "🌍", "🌙", "☀️",
        "⌚", "📱", "💻", "📚", "🎁", "🎉"
    )

    fun isEmojiOnly(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 16) return false
        return trimmed.none { it.isLetterOrDigit() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HereEmojiPickerSheet(
    recentEmojis: List<String>,
    onSelectEmoji: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("Recent", "Frequent", "Smileys", "Gestures", "Hearts", "Objects")
    var selectedCategory by remember { mutableStateOf("Frequent") }

    val activeList = when (selectedCategory) {
        "Recent" -> recentEmojis.ifEmpty { HereEmojiCatalog.frequent }
        "Frequent" -> HereEmojiCatalog.frequent
        "Smileys" -> HereEmojiCatalog.smileys
        "Gestures" -> HereEmojiCatalog.gestures
        "Hearts" -> HereEmojiCatalog.hearts
        else -> HereEmojiCatalog.objects
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Emoji",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeList) { emoji ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectEmoji(emoji) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
