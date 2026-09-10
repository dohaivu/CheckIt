package com.checkit.ui.quicknote

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeQuickNoteImage(path: String): ImageBitmap? =
    try {
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
