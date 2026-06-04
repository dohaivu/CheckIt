package com.checkit.platform

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

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
        actual val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
        actual fun getPlatform(): Platforms {
            return Platforms.iOS
        }

        @OptIn(ExperimentalForeignApi::class)
        actual fun getFileLogWriter(): LogWriter {
            val logFilePath = documentDirectory() + "/logs"
            if (!NSFileManager.defaultManager.fileExistsAtPath(logFilePath)) {
                NSFileManager.defaultManager.createDirectoryAtPath(logFilePath, true, null, null)
            }
            val config = RollingFileLogWriterConfig(
                logFileName = "lingua_log",
                logFilePath = Path(logFilePath),
                rollOnSize = 10 * 1024 * 1024, // 10MB file size limit
                maxLogFiles = 1
            )
            return RollingFileLogWriter(config)
        }

        actual fun shareLogFile() {
            val logFilePath = documentDirectory() + "/logs/lingua_log.log"
            if (!NSFileManager.defaultManager.fileExistsAtPath(logFilePath)) {
                Logger.w("shareLogFile") { "Log file does not exist, cannot share." }
                return
            }

            val logFileURL = NSURL.fileURLWithPath(logFilePath)
            val activityItems = listOf(logFileURL)

            val activityViewController = UIActivityViewController(
                activityItems = activityItems,
                applicationActivities = null
            )

            val window = (UIApplication.sharedApplication.connectedScenes.first() as? UIWindowScene)?.windows?.first() as? UIWindow
            var rootViewController = window?.rootViewController
            while (rootViewController?.presentedViewController != null) {
                rootViewController = rootViewController.presentedViewController
            }

            rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
        }
    }
}