package com.checkit.ui.quicknote

/**
 * Platform camera capture for image notes. The Android implementation
 * launches the system camera, converts the photo to WEBP, and returns a
 * device-local file path; iOS is a NoOp until implemented.
 */
interface QuickNoteCameraCapture {
    fun capture(onResult: (localPath: String?) -> Unit)
    fun discard(localPath: String)
}

class NoOpQuickNoteCameraCapture : QuickNoteCameraCapture {
    override fun capture(onResult: (localPath: String?) -> Unit) = onResult(null)
    override fun discard(localPath: String) = Unit
}
