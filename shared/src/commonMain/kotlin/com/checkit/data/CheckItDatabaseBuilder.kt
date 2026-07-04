package com.checkit.data

import androidx.room3.RoomDatabase

expect fun provideDatabaseBuilder(): RoomDatabase.Builder<CheckItDatabase>

