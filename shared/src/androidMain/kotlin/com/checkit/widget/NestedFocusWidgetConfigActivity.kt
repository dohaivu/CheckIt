package com.checkit.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.checkit.domain.usecase.ObserveNestedDocumentsUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Widget configuration: pick which nested document the focus widget shows.
 * Mandatory at placement time (system requirement for configure activities).
 */
class NestedFocusWidgetConfigActivity : ComponentActivity(), KoinComponent {

    private val observeDocuments: ObserveNestedDocumentsUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        // Default result: backing out cancels placement.
        setResult(RESULT_CANCELED)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val documents by observeDocuments().collectAsState(initial = emptyList())
                    var selectedId by remember { mutableStateOf<String?>(null) }
                    var hasExisting by remember { mutableStateOf(false) }
                    var maxDepth by remember { mutableStateOf(NESTED_FOCUS_DEFAULT_DEPTH) }
                    // Preselect stored choices when reconfiguring an
                    // existing widget (no-op on first placement).
                    LaunchedEffect(appWidgetId) {
                        readDocumentId(appWidgetId)?.let {
                            selectedId = it
                            hasExisting = true
                        }
                        maxDepth = readMaxDepth(appWidgetId)
                    }
                    Column(modifier = Modifier.statusBarsPadding().padding(16.dp)) {
                        Text(
                            text = "Show high-priority items from:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (documents.isEmpty()) {
                            Text(
                                text = "No documents yet. Create one in the app first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(modifier = Modifier.weight(0.5f)) {
                                items(documents, key = { it.id }) { doc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedId = doc.id }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedId == doc.id,
                                            onClick = { selectedId = doc.id }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = doc.title.ifBlank { "Untitled" },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Levels of descendants",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1..3).forEach { level ->
                                Row(
                                    modifier = Modifier
                                        .clickable { maxDepth = level }
                                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = maxDepth == level,
                                        onClick = { maxDepth = level }
                                    )
                                    Text(text = "$level")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { finish() }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val id = selectedId ?: return@Button
                                    confirm(appWidgetId, id, maxDepth)
                                },
                                enabled = selectedId != null
                            ) {
                                Text(if (hasExisting) "Save" else "Add widget")
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun readDocumentId(appWidgetId: Int): String? = runCatching {
        val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(appWidgetId)
        NestedFocusWidget().getAppWidgetState<Preferences>(applicationContext, glanceId)[NestedFocusDocumentKey]
    }.getOrNull()

    private suspend fun readMaxDepth(appWidgetId: Int): Int = runCatching {
        val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(appWidgetId)
        NestedFocusWidget().getAppWidgetState<Preferences>(applicationContext, glanceId)[NestedFocusDepthKey]
    }.getOrNull() ?: NESTED_FOCUS_DEFAULT_DEPTH

    private fun confirm(appWidgetId: Int, documentId: String, maxDepth: Int) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(applicationContext, glanceId) {
                it[NestedFocusDocumentKey] = documentId
                it[NestedFocusDepthKey] = maxDepth
            }
            NestedFocusWidget().update(applicationContext, glanceId)
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, result)
            finish()
        }
    }
}
