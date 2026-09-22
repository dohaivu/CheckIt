package com.checkit.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.TagItem
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.MarkdownToolbar
import com.checkit.ui.components.MarkdownVisualTransformation
import com.checkit.ui.components.TagPicker
import com.checkit.ui.myday.JournalEntryEditorState

private enum class JournalEditorMode { Write, Details }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun JournalEntryEditorSheet(
    state: JournalEntryEditorState,
    availableTags: List<TagItem>,
    nowMinutes: Int,
    onDismiss: () -> Unit,
    onLabelChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onLabelSelected: (String) -> Unit,
    onPromptSelected: (JournalPrompt) -> Unit,
    onPromptCleared: () -> Unit,
    onDiscardDraft: () -> Unit,
    onMoodToggle: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onNewTagClick: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.ime),
        sheetGesturesEnabled = false
    ) {
        var mode by remember { mutableStateOf(JournalEditorMode.Write) }
        var labelFocused by remember { mutableStateOf(false) }
        val contentFocusRequester = remember { FocusRequester() }

        // Cursor-aware content state. Tracks selection locally so the toolbar
        // can insert at the cursor instead of appending at the end.
        var contentValue by remember(state.entryId) {
            mutableStateOf(TextFieldValue(state.content, TextRange(state.content.length)))
        }
        var lastExternalContent by remember(state.entryId) { mutableStateOf(state.content) }
        if (state.content != lastExternalContent && state.content != contentValue.text) {
            contentValue = TextFieldValue(state.content, TextRange(state.content.length))
            lastExternalContent = state.content
        }

        val wordCount = remember(state.content) { countJournalWords(state.content) }
        val readMinutes = remember(wordCount) { journalReadMinutes(wordCount) }
        val followUp = remember(wordCount) { followUpForWords(wordCount) }
        val activePrompt = remember(state.promptId) { findJournalPrompt(state.promptId) }
        val isFeelingPrompt = activePrompt?.category == JournalPromptCategory.StateOfMind

        // Header: draft status + save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ModeToggle(mode = mode, onModeChange = { mode = it })

            if (state.isDraftResume && !state.isEditMode) {
                TextButton(onClick = onDiscardDraft) {
                    Text("Discard Draft")
                }

            }
            Button(onClick = onSave) {
                Text(if (state.isEditMode) "Save" else "Add Entry")
            }
            if (state.isEditMode) {
                DeleteOverflowMenu(onDelete = onDelete)
            }
        }

        if (mode == JournalEditorMode.Write) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PromptPicker(
                            selectedPromptId = state.promptId,
                            nowMinutes = nowMinutes,
                            onPromptSelected = onPromptSelected,
                            onClear = onPromptCleared,
                            selectedMoods = state.moods
                        )
                        if (state.content.isBlank() && state.promptId == null) {
                            Text(
                                text = "Pick a prompt to get started",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column {
                            if (state.prompt.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = state.prompt,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onPromptCleared, modifier = Modifier.padding(start = 4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear prompt",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            AppOutlinedTextField(
                                value = contentValue,
                                onValueChange = { next ->
                                    contentValue = next
                                    if (next.text != state.content) {
                                        lastExternalContent = next.text
                                        onContentChange(next.text)
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 24.sp,
                                    fontSize = 16.sp
                                ),
                                placeholder = if (state.promptId != null) {
                                    if (isFeelingPrompt) "How are you, really? Take your time…" else "Follow the prompt above, write freely…"
                                } else {
                                    "How are you, really? Write freely — you can resume later…"
                                },
                                minLines = 12,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(contentFocusRequester),
                                visualTransformation = remember { MarkdownVisualTransformation() }
                            )
                        }
                    }
                }
            }

            // Static toolbar above footer: stays pinned while content scrolls
            MarkdownToolbar(
                onAction = { action ->
                    val edit = applyJournalToolbarAction(
                        text = contentValue.text,
                        selectionStart = contentValue.selection.start,
                        selectionEnd = contentValue.selection.end,
                        action = action
                    )
                    contentValue = contentValue.copy(
                        text = edit.text,
                        selection = TextRange(edit.selectionStart, edit.selectionEnd)
                    )
                    lastExternalContent = edit.text
                    onContentChange(edit.text)
                    contentFocusRequester.requestFocus()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Sticky footer: word count + reading time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        wordCount == 0 -> "0 words — start writing"
                        readMinutes <= 1 -> "$wordCount words"
                        else -> "$wordCount words • ~$readMinutes min"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (followUp != null) {
                    Text(
                        text = followUp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        AppOutlinedTextField(
                            value = state.label,
                            onValueChange = onLabelChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            ),
                            placeholder = "Add label",
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { labelFocused = it.isFocused }
                        )
                        if (labelFocused) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                JournalLabels.forEach { label ->
                                    LabelChip(
                                        label = label,
                                        selected = state.label == label,
                                        onClick = {
                                            onLabelSelected(label)
                                            labelFocused = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    MoodRow(
                        moods = state.moods.toSet(),
                        onToggle = onMoodToggle,
                        isEditMode = state.isEditMode
                    )
                }
                item {
                    FeelingChips(
                        moods = state.moods,
                        contentBlank = contentValue.text.isBlank(),
                        onFeelingSelected = { word ->
                            val starter = "I feel $word because… "
                            val base = contentValue.text
                            val newText = if (base.isBlank()) starter else {
                                if (base.endsWith("\n") || base.endsWith(" ")) base + starter else "$base\n$starter"
                            }
                            contentValue = contentValue.copy(
                                text = newText,
                                selection = TextRange(newText.length)
                            )
                            lastExternalContent = newText
                            onContentChange(newText)
                        }
                    )
                }
                item {
                    TagPicker(
                        availableTags = availableTags,
                        selectedTagIds = state.selectedTagIds,
                        onTagToggle = onTagToggle,
                        onNewTagClick = onNewTagClick
                    )
                }
                item {
                    Text(
                        text = "$wordCount words" + if (readMinutes > 1) " • ~$readMinutes min" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    mode: JournalEditorMode,
    onModeChange: (JournalEditorMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        JournalEditorMode.entries.forEach { entry ->
            val selected = entry == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.0f))
                    .clickable { onModeChange(entry) }
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun LabelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeelingChips(
    moods: List<String>,
    contentBlank: Boolean,
    onFeelingSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val words = remember(moods) { feelingWordsForMoods(moods) }
    if (words.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (contentBlank) "Put it into words:" else "Refine it:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            words.take(12).forEach { word ->
                LabelChip(label = word, selected = false, onClick = { onFeelingSelected(word) })
            }
        }
    }
}



internal fun insertJournalSnippet(content: String, snippet: String): String {
    if (snippet.isEmpty()) return content
    if (content.isEmpty()) return snippet.trimStart('\n')
    // Block snippets already carry a leading newline.
    if (snippet.startsWith("\n")) {
        return if (content.endsWith("\n")) content + snippet.trimStart('\n') else content + snippet
    }
    return content + snippet
}
