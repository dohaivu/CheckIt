package com.checkit.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

data object HelpContent {
    val okrTips: String = """
        #### __1. Work Backwards (The "How" Rule)__

        - *Dream:* I want to understand intermediate Spanish. ➔ *How?* ➔ **Objective:** Learn a ton of vocabulary.
        
        - *Objective:* Learn vocabulary. ➔ *How do I measure that?* ➔ **Key Result:** Get 1,000 words into my deck.
        
        - *Key Result:* 1,000 words. ➔ *How do I execute that today?* ➔ **Task:** Open the app for 15 minutes.
        
        #### __2. If It Doesn't Have a Number, It's Not a Key Result__
        
        ❌ *Bad KR*: "Read Spanish book." (This is just a task).
        
        ✅ *Good KR*: "Read 15 chapters of Spanish book."
    """.trimIndent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTooltip(
    modifier: Modifier = Modifier,
    markdownContent: String
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val title = "OKR Tips"

    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Below),
        tooltip = {
            RichTooltip(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                action = null,
            ) {
                RichTextPreview(
                    markdown = markdownContent,
                    modifier = modifier.verticalScroll(scrollState)
                )
            }
        },
        state = tooltipState
    ) {
        IconButton(onClick = {
            coroutineScope.launch {
                tooltipState.show()
            }
        }) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Help"
            )
        }
    }
}
