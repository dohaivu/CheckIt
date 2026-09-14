package com.checkit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.checkit.data.QuickNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class QuickNoteReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val repository: QuickNoteRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val pendingResult = goAsync()
        scope.launch {
            try {
                CheckItNotificationCenter(context.applicationContext)
                    .showQuickNoteReminder(noteId, content)
                runCatching { repository.clearReminder(noteId) }
            } catch (e: Exception) {
                Log.e("QuickNoteReminder", "Failed to show reminder for $noteId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.checkit.ACTION_QUICK_NOTE_REMIND"
        const val EXTRA_NOTE_ID = "quick_note_id"
        const val EXTRA_CONTENT = "quick_note_content"
    }
}
