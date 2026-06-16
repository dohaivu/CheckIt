package com.checkit

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.checkit.notifications.CheckItNotificationCenter
import com.checkit.ui.CheckItApp
import com.checkit.widget.ExtraDailyPlanItemId
import com.checkit.widget.ExtraNoteId
import com.checkit.widget.ExtraOpenMyDaySuggestions
import com.checkit.widget.ExtraTaskId

class MainActivity : ComponentActivity() {
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private val dailyPlanItemLaunchId = mutableStateOf<Long?>(null)
    private val taskLaunchId = mutableStateOf<Long?>(null)
    private val noteLaunchId = mutableStateOf<Long?>(null)
    private val openMyDaySuggestionsLaunch = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }

        CheckItNotificationCenter(this).ensureChannels()

        setContent {
            CheckItApp(
                dailyPlanItemLaunchId = dailyPlanItemLaunchId.value,
                taskLaunchId = taskLaunchId.value,
                noteLaunchId = noteLaunchId.value,
                openMyDaySuggestionsLaunch = openMyDaySuggestionsLaunch.value,
                onWidgetLaunchConsumed = ::clearWidgetLaunch
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Removed notificationManager.cancelAll() to prevent clearing reminders automatically
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun handleLaunchIntent(intent: Intent) {
        dailyPlanItemLaunchId.value = intent.longExtraOrNull(ExtraDailyPlanItemId)
        taskLaunchId.value = intent.longExtraOrNull(ExtraTaskId)
        noteLaunchId.value = intent.longExtraOrNull(ExtraNoteId)
        openMyDaySuggestionsLaunch.value = intent.getBooleanExtra(ExtraOpenMyDaySuggestions, false)
    }

    private fun clearWidgetLaunch() {
        dailyPlanItemLaunchId.value = null
        taskLaunchId.value = null
        noteLaunchId.value = null
        openMyDaySuggestionsLaunch.value = false
    }
}

private fun Intent.longExtraOrNull(name: String): Long? =
    getLongExtra(name, MissingLaunchId).takeIf { it != MissingLaunchId }

private const val MissingLaunchId = -1L
