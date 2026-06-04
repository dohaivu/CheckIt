package com.checkit.data

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun provideDatabaseBuilder(): RoomDatabase.Builder<CheckItDatabase> {
    return Room.databaseBuilder<CheckItDatabase>(
        name = appDatabasePath("checkit.db")
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
