package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TinyTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier.Companion,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val actualContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    CompositionLocalProvider(LocalContentColor provides actualContentColor) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(containerColor ?: MaterialTheme.colorScheme.background)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            navigationIcon()
            Box(
                modifier = Modifier.Companion
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Companion.CenterStart
            ) {
                title()
            }
            actions()
        }
    }
}
