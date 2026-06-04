package com.checkit.infrastructure

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Single shared Context provider through Koin injection.
 * Replaces MainApplication.instance singleton pattern.
 */
object AndroidContextProvider : KoinComponent {
    val context: Context by inject()
}
