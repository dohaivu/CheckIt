package com.checkit.auth

import kotlinx.coroutines.flow.StateFlow

data class GoogleAccountState(
    /** Null while anonymous (the default) or signed out. */
    val email: String? = null,
    val isAnonymous: Boolean = true,
    val busy: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Google Sign-In for sync identity. Anonymous stays the default; signing in
 * links the Google credential onto the anonymous user so the UID (and
 * therefore users/{uid}/quickNotes) is preserved — no data migration.
 * A second device signing in with the same Google account lands on the same
 * UID and syncs the same collection.
 */
interface GoogleAccountManager {
    val accountState: StateFlow<GoogleAccountState>
    suspend fun signIn()
    suspend fun signOut()
    fun clearError()
}
