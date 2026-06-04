package com.checkit.data

import androidx.room.RoomDatabase

expect fun provideDatabaseBuilder(): RoomDatabase.Builder<CheckItDatabase>

