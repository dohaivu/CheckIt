package com.checkit.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

/** Shared await for Google Play Services Tasks (auth, Firestore, Storage). */
internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener {
        if (cont.isActive) cont.resume(it, null)
    }
    addOnFailureListener {
        if (cont.isActive) cont.resumeWithException(it)
    }
}
