package com.checkit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.checkit.domain.usecase.SprintTransitionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.util.Log

class SprintActionReceiver : BroadcastReceiver(), KoinComponent {
    private val transitionUseCase: SprintTransitionUseCase by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_SAVE_WIN = "com.checkit.ACTION_SAVE_WIN"
        const val ACTION_SAVE_BREAK = "com.checkit.ACTION_SAVE_BREAK"
        const val ACTION_SAVE_CONTINUE = "com.checkit.ACTION_SAVE_CONTINUE"
        const val ACTION_START_NEXT = "com.checkit.ACTION_START_NEXT"
        const val ACTION_UPGRADE = "com.checkit.ACTION_UPGRADE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("SprintAction", "Received action: $action")
        val pendingResult = goAsync()
        
        scope.launch {
            try {
                when (action) {
                    ACTION_SAVE_WIN -> transitionUseCase.saveWin()
                    ACTION_SAVE_BREAK -> transitionUseCase.saveAndBreak()
                    ACTION_SAVE_CONTINUE -> transitionUseCase.saveAndContinue()
                    ACTION_START_NEXT -> transitionUseCase.startNext()
                    ACTION_UPGRADE -> transitionUseCase.upgradeToPomodoro()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
