package com.checkit.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.checkit.infrastructure.AndroidContextProvider

fun getCheckItDatabaseBuilder(context: Context): RoomDatabase.Builder<CheckItDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<CheckItDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("checkit.db").absolutePath
    )
}

actual fun provideDatabaseBuilder(): RoomDatabase.Builder<CheckItDatabase> {
    return getCheckItDatabaseBuilder(AndroidContextProvider.context)
}

