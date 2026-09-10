package com.checkit.ui.quicknote

import com.checkit.domain.usecase.CreateQuickNoteUseCase
import com.checkit.domain.usecase.DeleteQuickNotePermanentlyUseCase
import com.checkit.domain.usecase.MaintainQuickNotesUseCase
import com.checkit.domain.usecase.MoveQuickNoteToBeDeletedUseCase
import com.checkit.domain.usecase.ObserveQuickNextUseCase
import com.checkit.domain.usecase.ObserveQuickToBeDeletedUseCase
import com.checkit.domain.usecase.RestoreQuickNoteUseCase
import com.checkit.domain.usecase.SetQuickNoteReminderUseCase
import com.checkit.infrastructure.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.mp.KoinPlatform

/**
 * Swift-friendly facade over the QuickNote use cases for the macOS
 * MenuBarExtra app (SwiftUI cannot consume suspend functions or
 * [kotlinx.coroutines.flow.Flow] directly without SKIE).
 *
 * All callbacks are delivered on the Main thread.
 */
class QuickNoteSubscription internal constructor(
    private val job: Job,
) {
    fun cancel() {
        job.cancel()
    }
}

class QuickNoteMenuHelper(
    private val observeNext: ObserveQuickNextUseCase,
    private val observeToBeDeleted: ObserveQuickToBeDeletedUseCase,
    private val createNote: CreateQuickNoteUseCase,
    private val moveToBeDeleted: MoveQuickNoteToBeDeletedUseCase,
    private val restoreNote: RestoreQuickNoteUseCase,
    private val deletePermanently: DeleteQuickNotePermanentlyUseCase,
    private val setReminder: SetQuickNoteReminderUseCase,
    private val maintain: MaintainQuickNotesUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun observeNotes(onUpdate: (List<com.checkit.domain.QuickNote>) -> Unit): QuickNoteSubscription {
        val job = scope.launch {
            observeNext().collect { notes ->
                onUpdate(notes)
            }
        }
        return QuickNoteSubscription(job)
    }

    fun observeDeletedNotes(onUpdate: (List<com.checkit.domain.QuickNote>) -> Unit): QuickNoteSubscription {
        val job = scope.launch {
            observeToBeDeleted().collect { notes ->
                onUpdate(notes)
            }
        }
        return QuickNoteSubscription(job)
    }

    fun create(content: String) {
        scope.launch {
            runCatching { createNote(content) }
        }
    }

    fun create(content: String, onDone: () -> Unit) {
        scope.launch {
            runCatching { createNote(content) }
            onDone()
        }
    }

    fun moveToTrash(id: String) {
        scope.launch {
            runCatching { moveToBeDeleted(id) }
        }
    }

    fun restore(id: String) {
        scope.launch {
            runCatching { restoreNote(id) }
        }
    }

    fun deletePermanently(id: String) {
        scope.launch {
            runCatching { deletePermanently(id) }
        }
    }

    /** Mirrors QuickNoteViewModel presets: duration counted from now. */
    fun setReminderIn(id: String, durationMillis: Long) {
        scope.launch {
            runCatching { setReminder(id, durationMillis) }
        }
    }

    /** Absolute fire time as epoch millis; ignored when not in the future. */
    fun setReminderAt(id: String, epochMillis: Long) {
        scope.launch {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val duration = epochMillis - now
            if (duration > 0) {
                runCatching { setReminder(id, duration) }
            }
        }
    }

    fun clearReminder(id: String) {
        scope.launch {
            runCatching { setReminder.clear(id) }
        }
    }

    /**
     * Full maintenance, mirroring QuickNoteViewModel.refresh():
     * auto-trash inactive notes, expire 24h items, clear fired
     * reminders, reconcile alarms, and sync.
     */
    fun refresh() {
        scope.launch {
            runCatching { maintain() }
        }
    }

    fun close() {
        scope.cancel()
    }
}

object QuickNoteAppleBridge {
    private var koinApp: KoinApplication? = null

    fun ensureKoin() {
        if (KoinPlatform.getKoinOrNull() == null) {
            koinApp = runCatching { initKoin() }.getOrNull()
        }
    }

    fun menuHelper(): QuickNoteMenuHelper =
        koinApp?.koin?.get() ?: KoinPlatform.getKoin().get()

    fun syncBridge(): com.checkit.data.QuickNoteSyncBridge =
        koinApp?.koin?.get() ?: KoinPlatform.getKoin().get()
}
