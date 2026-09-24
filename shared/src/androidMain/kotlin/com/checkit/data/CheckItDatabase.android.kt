package com.checkit.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.checkit.infrastructure.AndroidContextProvider

fun getCheckItDatabaseBuilder(context: Context): RoomDatabase.Builder<CheckItDatabase> {
    val appContext = context.applicationContext
    val path = appContext.getDatabasePath("checkit.db").absolutePath
    println("CheckIt database path: $path")
    return Room.databaseBuilder<CheckItDatabase>(
        context = appContext,
        name = path
    )
}

actual fun provideDatabaseBuilder(): RoomDatabase.Builder<CheckItDatabase> {
    return getCheckItDatabaseBuilder(AndroidContextProvider.context)
}

