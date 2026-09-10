package com.checkit.ui.quicknote

import com.checkit.domain.usecase.CreateQuickNoteUseCase
import com.checkit.domain.usecase.DeleteQuickNotePermanentlyUseCase
import com.checkit.domain.usecase.MoveQuickNoteToBeDeletedUseCase
import com.checkit.domain.usecase.ObserveQuickNextUseCase
import com.checkit.domain.usecase.RestoreQuickNoteUseCase
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
    private val createNote: CreateQuickNoteUseCase,
    private val moveToBeDeleted: MoveQuickNoteToBeDeletedUseCase,
    private val restoreNote: RestoreQuickNoteUseCase,
    private val deletePermanently: DeleteQuickNotePermanentlyUseCase,
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
}
