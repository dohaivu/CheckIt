package com.checkit.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hooks invoked from Swift. Kotlin cannot call Swift directly, so Swift
 * registers its sync entry point here once at startup
 * (QuickNoteFirestoreSync.start()).
 */
object AppleSyncHooks {
    var requestSync: (() -> Unit)? = null
}

/**
 * macOS QuickNoteSyncManager: forwards triggering to Swift's
 * QuickNoteFirestoreSync, making sync triggering repository-owned exactly
 * like Android — every repository mutation calls requestSync() internally,
 * so no Swift call site has to remember it.
 *
 * requestSync() is debounced Swift-side. sync() is fire-and-forget: Kotlin
 * cannot suspend until Swift work completes, so callers must not depend on
 * its completion (MaintainQuickNotesUseCase already doesn't).
 */
class AppleQuickNoteSyncManager : QuickNoteSyncManager {
    private val _syncState = MutableStateFlow(QuickNoteSyncState())
    override val syncState: StateFlow<QuickNoteSyncState> = _syncState.asStateFlow()

    override fun requestSync() {
        AppleSyncHooks.requestSync?.invoke()
    }

    override suspend fun sync() {
        AppleSyncHooks.requestSync?.invoke()
    }
}
