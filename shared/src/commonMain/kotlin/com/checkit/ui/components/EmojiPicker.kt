package com.checkit.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kodein.emoji.Emoji
import org.kodein.emoji.list


@Composable
fun EmojiPicker(
    modifier: Modifier = Modifier,
    onEmojiSelect: (Emoji) -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = {  },
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.ime),
        sheetGesturesEnabled = true
    ) {
        EmojiGrid(
            onEmojiSelect = onEmojiSelect
        )
    }
}

@Composable
fun EmojiGrid(
    modifier: Modifier = Modifier,
    onEmojiSelect: (Emoji) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 32.dp),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp)
    ) {
        items(Emoji.list()) { emoji ->
            IconButton(
                onClick = { onEmojiSelect(emoji) },
                modifier = Modifier.size(32.dp)
            ) {
                Text(text = emoji.details.string, fontSize = 20.sp)
            }
        }
    }
}