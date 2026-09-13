package com.checkit.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Pushes fresh data to the QuickNote widget — and the agenda's quick-note
 * section, which reads the same list. No-op when no instances are placed.
 */
suspend fun Context.updateQuickNoteWidgets() {
    runCatching { QuickNoteSingleWidget().updateAll(this) }
    runCatching { DailyPlanAgendaWidget().updateAll(this) }
}
