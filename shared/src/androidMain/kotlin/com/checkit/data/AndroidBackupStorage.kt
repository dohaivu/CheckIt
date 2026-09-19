package com.checkit.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android-only SAF folder storage for JSON backups.
 *
 * Backups live in the user-selected folder (mirroring SpendWise's
 * cloud-folder backup), stored as a single `CheckIt_Backup.json` file
 * that is overwritten on every backup.
 */
class AndroidBackupStorage(
    private val context: Context,
) {
    /** Display name of a SAF tree folder, for settings subtitles. */
    fun folderDisplayName(folderUri: String): String? {
        val uri = Uri.parse(folderUri)
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return null
        val treeUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
        return context.contentResolver.query(
            treeUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    /** Writes the JSON backup to the single backup file in the SAF folder, overwriting it. */
    suspend fun writeToFolder(folderUri: String, json: String): String = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            ?: error("Cannot access backup folder")
        val file = folder.findFile(BACKUP_FILE_NAME)
            ?: folder.createFile("application/json", BACKUP_FILE_NAME)
            ?: error("Cannot create backup file")
        context.contentResolver.openOutputStream(file.uri)?.use { output ->
            output.write(json.toByteArray())
        } ?: error("Cannot write backup file")
        BACKUP_FILE_NAME
    }

    /** Reads the backup file from the SAF folder, if any. */
    suspend fun readBackupFromFolder(folderUri: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                ?: return@withContext null
            val file = folder.findFile(BACKUP_FILE_NAME)
                ?.takeIf { it.isFile }
                ?: return@withContext null
            val json = context.contentResolver.openInputStream(file.uri)?.use { input ->
                input.bufferedReader().use { it.readText() }
            } ?: return@withContext null
            BACKUP_FILE_NAME to json
        }

    companion object {
        const val BACKUP_FILE_NAME = "CheckIt_Backup.json"
    }
}
