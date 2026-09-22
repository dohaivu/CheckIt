package com.checkit.data

/**
 * Single source for nested-list Firestore sync topology. Mirrors
 * [QuickNoteSyncConfig]: everything addressing remote resources lives here
 * so Android (`FirestoreNestedSyncManager`) and macOS (Swift
 * `NestedFirestoreSync`) can never diverge. Timing policies stay
 * platform-local; retention reuses QuickNote's tombstone policy.
 */
object NestedSyncConfig {
    const val DATABASE_ID = QuickNoteSyncConfig.DATABASE_ID
    const val USERS_COLLECTION = QuickNoteSyncConfig.USERS_COLLECTION
    const val DOCUMENTS_COLLECTION = "nestedDocuments"
    const val ITEMS_COLLECTION = "nestedItems"
    const val PUSH_BATCH_SIZE = 400
}
