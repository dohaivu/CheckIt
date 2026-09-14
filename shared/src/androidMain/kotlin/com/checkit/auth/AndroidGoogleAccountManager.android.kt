package com.checkit.auth

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.checkit.data.QuickNoteSyncManager
import com.checkit.util.awaitTask
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the foreground Activity, which CredentialManager needs to present
 * its UI. Registered once from MainActivity, mirroring QuickNoteCameraHolder.
 */
object GoogleSignInHolder {
    @Volatile var activity: ComponentActivity? = null

    fun init(activity: ComponentActivity) {
        this.activity = activity
    }
}

class AndroidGoogleAccountManager(
    appContext: Context,
    private val syncManager: QuickNoteSyncManager,
) : GoogleAccountManager {
    private val app = appContext.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(currentState())
    override val accountState: StateFlow<GoogleAccountState> = _state.asStateFlow()

    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _state.update { it.copy(email = user?.email, isAnonymous = user?.isAnonymous ?: true) }
    }

    init {
        auth.addAuthStateListener(listener)
    }

    private fun currentState(): GoogleAccountState {
        val user = auth.currentUser
        return GoogleAccountState(email = user?.email, isAnonymous = user?.isAnonymous ?: true)
    }

    override suspend fun signIn() {
        val activity = GoogleSignInHolder.activity
        if (activity == null) {
            _state.update { it.copy(errorMessage = "Sign-in needs a foreground screen. Reopen the app and retry.") }
            return
        }
        _state.update { it.copy(busy = true, errorMessage = null) }
        try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val result = CredentialManager.create(activity).getCredential(activity, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val user = auth.currentUser
            if (user != null && user.isAnonymous) {
                // Link: the anonymous UID is preserved, data stays in place.
                try {
                    user.linkWithCredential(firebaseCredential).awaitTask()
                } catch (e: FirebaseAuthUserCollisionException) {
                    // Google account already linked elsewhere (e.g. signed in
                    // on another device first): join that account instead.
                    // Local Room rows merge in via the next dirty push.
                    val updated = e.updatedCredential
                    if (updated != null) {
                        auth.signInWithCredential(updated).awaitTask()
                    } else {
                        throw e
                    }
                }
            } else {
                auth.signInWithCredential(firebaseCredential).awaitTask()
            }
            _state.update { it.copy(busy = false) }
            // The UID may have changed (merge path): sync the new collection.
            syncManager.requestSync()
        } catch (e: CancellationException) {
            _state.update { it.copy(busy = false) }
            throw e
        } catch (e: GetCredentialCancellationException) {
            // User dismissed the account picker; not an error.
            _state.update { it.copy(busy = false) }
        } catch (e: Exception) {
            Log.w(TAG, "Google sign-in failed", e)
            _state.update { it.copy(busy = false, errorMessage = e.message ?: "Sign-in failed") }
        }
    }

    override suspend fun signOut() {
        _state.update { it.copy(busy = true, errorMessage = null) }
        try {
            runCatching { CredentialManager.create(app).clearCredentialState(ClearCredentialStateRequest()) }
            auth.signOut()
            // Next sync falls back to a fresh anonymous user (offline-first).
            syncManager.requestSync()
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    override fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "GoogleSignIn"
        /**
         * OAuth client (type 3, Web) from google-services.json. Public value,
         * not a secret; must match the Firebase project's web client.
         */
        private const val WEB_CLIENT_ID =
            "1046828436758-be04cbqhm4rb8ua5ohlfoe802o86tlg4.apps.googleusercontent.com"
    }
}
