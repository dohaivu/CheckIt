package com.checkit.platform

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.AppKit.NSWorkspace
import platform.AppKit.NSApplication

@OptIn(ExperimentalForeignApi::class)
fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}

actual class Platform {
    actual companion object {
        actual val name: String = "macOS " + NSProcessInfo.processInfo.operatingSystemVersionString
        actual fun getPlatform(): Platforms {
            return Platforms.macOS
        }

        @OptIn(ExperimentalForeignApi::class)
        actual fun getFileLogWriter(): LogWriter {
            val logFilePath = documentDirectory() + "/logs"
            if (!NSFileManager.defaultManager.fileExistsAtPath(logFilePath)) {
                NSFileManager.defaultManager.createDirectoryAtPath(logFilePath, true, null, null)
            }
            val config = RollingFileLogWriterConfig(
                logFileName = "checkit_log",
                logFilePath = Path(logFilePath),
                rollOnSize = 10 * 1024 * 1024,
                maxLogFiles = 1
            )
            return RollingFileLogWriter(config)
        }

        actual fun shareLogFile() {
            val logFilePath = documentDirectory() + "/logs/checkit_log.log"
            if (!NSFileManager.defaultManager.fileExistsAtPath(logFilePath)) {
                Logger.w("shareLogFile") { "Log file does not exist, cannot share." }
                return
            }

            val logFileURL = NSURL.fileURLWithPath(logFilePath)
            NSWorkspace.sharedWorkspace().activateFileViewerSelectingURLs(listOf(logFileURL))
        }

        actual fun isAppInForeground(): Boolean {
            return NSApplication.sharedApplication.active
        }
    }
}
