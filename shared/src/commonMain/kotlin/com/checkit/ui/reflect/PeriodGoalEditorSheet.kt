package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.reflect_review_card_title
import checkit.shared.generated.resources.reflect_review_save
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.MarkdownVisualTransformation
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodGoalEditorSheet(
    editor: ReflectGoalEditorState,
    onReviewChange: (String) -> Unit,
    onGoalChange: (String) -> Unit,
    onRatingsChange: (Float) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val periodLabel = editor.focus.period.label()
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        sheetGesturesEnabled = !editor.isSaving,
        modifier = Modifier
            .fillMaxHeight(0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = editor.focus.period.reviewIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.reflect_review_card_title, periodLabel).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "What are your wins? frictions? lessons?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AppOutlinedTextField(
                value = editor.review,
                onValueChange = onReviewChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                placeholder = "Jot down your reflection ...",
                minLines = 8,
                enabled = !editor.isSaving,
                visualTransformation = remember { MarkdownVisualTransformation() }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "SATISFACTION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = ((editor.ratings * 10).roundToInt() / 10f).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Slider(
                value = editor.ratings,
                onValueChange = onRatingsChange,
                valueRange = 0f..5f,
                steps = 9,
                enabled = !editor.isSaving
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "PERIOD GOAL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "What are the 3 non-negotiable priority tasks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AppOutlinedTextField(
                value = editor.goal,
                onValueChange = onGoalChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                placeholder = "What will you focus on?",
                minLines = 5,
                enabled = !editor.isSaving,
                visualTransformation = remember { MarkdownVisualTransformation() }
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.reflect_review_save))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}