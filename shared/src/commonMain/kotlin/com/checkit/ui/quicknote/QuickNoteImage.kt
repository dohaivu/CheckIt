package com.checkit.ui.quicknote

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes a device-local image file for preview. Returns null when the
 * file is missing or undecodable; iOS returns null (not implemented).
 */
expect fun decodeQuickNoteImage(path: String): ImageBitmap?
