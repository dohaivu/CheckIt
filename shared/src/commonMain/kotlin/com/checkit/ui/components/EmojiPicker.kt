package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kodein.emoji.Emoji
import org.kodein.emoji.list


@Composable
fun EmojiPicker(
    onDismiss: () -> Unit,
    onEmojiSelect: (Emoji) -> Unit,
    modifier: Modifier = Modifier
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.ime),
        sheetGesturesEnabled = true
    ) {
        EmojiGrid(
            onEmojiSelect = {
                onEmojiSelect(it)
                onDismiss()
            }
        )
    }
}

@Composable
fun EmojiGrid(
    modifier: Modifier = Modifier,
    onEmojiSelect: (Emoji) -> Unit
) {
    val emojis = remember { Emoji.list() }
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 48.dp),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp)
    ) {
        items(
            items = emojis,
            key = { it.details.string },
            contentType = { "emoji" }
        ) { emoji ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onEmojiSelect(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji.details.string, fontSize = 26.sp)
            }
        }
    }
}