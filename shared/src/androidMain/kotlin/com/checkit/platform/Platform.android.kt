package com.checkit.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import com.checkit.infrastructure.AndroidContextProvider
import kotlinx.io.files.Path
import java.io.File

actual class Platform {
    actual companion object {
        actual val name: String = "Android ${Build.VERSION.SDK_INT}"
        actual fun getPlatform(): Platforms {
            return Platforms.Android
        }

        actual fun getFileLogWriter(): LogWriter {
            val context: Context = AndroidContextProvider.context
            val logDirectory = File(context.filesDir, "logs")
            if (!logDirectory.exists()) logDirectory.mkdirs()

            val config = RollingFileLogWriterConfig(
                logFileName = "lingua_log",
                logFilePath = Path(logDirectory.absolutePath),
                rollOnSize = 10 * 1024 * 1024, // 10MB file size limit
                maxLogFiles = 1
            )

            return RollingFileLogWriter(
                config = config
            )
        }

        actual fun shareLogFile() {
            val context: Context = AndroidContextProvider.context
            val logDirectory = File(context.filesDir, "logs")
            val logFile = File(logDirectory, "lingua_log.log")

            if (!logFile.exists()) {
                Logger.w("shareLogFile") { "Log file does not exist, cannot share." }
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, logFile)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Log File").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}