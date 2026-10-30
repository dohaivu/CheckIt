package com.checkit.ui.nested

import androidx.compose.ui.text.PlatformTextStyle

/**
 * Platform tweaks for the guide-dot emoji style. Android's legacy font padding
 * adds asymmetric space around emoji glyphs, which pushes them low enough to be
 * clipped by the dot's ring; trimming it keeps the glyph optically centred.
 * Other targets have no equivalent and keep the default metrics.
 */
internal expect fun guideEmojiPlatformStyle(): PlatformTextStyle?
