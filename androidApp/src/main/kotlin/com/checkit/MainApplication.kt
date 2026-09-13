package com.checkit

import android.app.Application
import android.util.Log
import com.checkit.android.BuildConfig
import com.checkit.domain.AppConfig
import com.checkit.domain.usecase.ObserveQuickNextUseCase
import com.checkit.infrastructure.initKoin
import com.checkit.widget.updateQuickNoteWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

class MainApplication: Application(), KoinComponent {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observeQuickNext: ObserveQuickNextUseCase by inject()

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(module {
                single { AppConfig(versionName = BuildConfig.VERSION_NAME) }
            })
        }

        observeQuickNotesForWidgets()

        Log.d("CheckIt", "MainApplication onCreate")
    }

    /**
     * Glance has no live data binding: widgets only re-render on update.
     * Push an update (debounced) whenever the NEXT list changes, so the
     * QuickNote widget — and the agenda's quick-note section — stay fresh.
     * updatePeriodMillis remains as fallback when the process is dead.
     */
    private fun observeQuickNotesForWidgets() {
        appScope.launch {
            observeQuickNext()
                .debounce(1.seconds)
                .distinctUntilChanged()
                .collect {
                    updateQuickNoteWidgets()
                }
        }
    }
}