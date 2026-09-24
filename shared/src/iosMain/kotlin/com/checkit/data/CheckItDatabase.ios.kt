package com.checkit.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun provideDatabaseBuilder(): RoomDatabase.Builder<CheckItDatabase> {
    val path = appDatabasePath("checkit.db")
    println("CheckIt database path: $path")
    return Room.databaseBuilder<CheckItDatabase>(
        name = path
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun appDatabasePath(name: String): String {
    val documentsDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return "${documentsDirectory?.path}/$name"
}
