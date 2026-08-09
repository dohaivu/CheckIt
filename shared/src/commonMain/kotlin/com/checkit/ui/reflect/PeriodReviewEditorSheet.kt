package com.checkit.ui.reflect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_content_placeholder
import checkit.shared.generated.resources.reflect_review_draft_note
import checkit.shared.generated.resources.reflect_review_generate_draft
import checkit.shared.generated.resources.reflect_review_intent_placeholder
import checkit.shared.generated.resources.reflect_review_save
import com.checkit.domain.ReviewSource
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodReviewEditorSheet(
    editor: ReflectReviewEditorState,
    onContentChange: (String) -> Unit,
    onIntentNextChange: (String) -> Unit,
    onGenerateDraft: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val periodLabel = editor.focus.period.label()
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        sheetGesturesEnabled = !editor.isSaving
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.reflect_review_card_title, periodLabel),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onGenerateDraft, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.reflect_review_generate_draft))
                }
            }
            if (editor.source == ReviewSource.Auto) {
                Text(
                    text = stringResource(Res.string.reflect_review_draft_note, periodLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AppOutlinedTextField(
                value = editor.content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.reflect_review_content_placeholder),
                minLines = 4,
                enabled = !editor.isSaving
            )
            AppOutlinedTextField(
                value = editor.intentNext,
                onValueChange = onIntentNextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.reflect_review_intent_placeholder),
                minLines = 2,
                enabled = !editor.isSaving
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSave, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.reflect_review_save))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}