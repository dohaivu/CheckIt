package com.checkit.platform

import co.touchlab.kermit.LogWriter

enum class Platforms {
    Android,
    iOS,
    JVM
}

expect class Platform {
    companion object {
        val name: String
        fun getPlatform(): Platforms

        fun getFileLogWriter(): LogWriter

        fun shareLogFile()
    }
}