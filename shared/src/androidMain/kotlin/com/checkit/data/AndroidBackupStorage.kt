package com.checkit.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android-only SAF folder storage for JSON backups.
 *
 * Backups live in the user-selected folder (mirroring SpendWise's
 * cloud-folder backup), written as timestamped `CheckIt_Backup_*.json` files.
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

    /** Writes a timestamped JSON backup into the SAF folder, returning the file name. */
    suspend fun writeToFolder(folderUri: String, json: String): String = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            ?: error("Cannot access backup folder")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = folder.createFile("application/json", "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION")
            ?: error("Cannot create backup file")
        context.contentResolver.openOutputStream(file.uri)?.use { output ->
            output.write(json.toByteArray())
        } ?: error("Cannot write backup file")
        file.name ?: "$BACKUP_PREFIX$timestamp$BACKUP_EXTENSION"
    }

    /** Reads the newest timestamped JSON backup from the SAF folder, if any. */
    suspend fun readNewestFromFolder(folderUri: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                ?: return@withContext null
            val newest = folder.listFiles()
                .filter { it.isFile && it.name.orEmpty().startsWith(BACKUP_PREFIX) }
                .maxByOrNull { it.lastModified() }
                ?: return@withContext null
            val name = newest.name ?: return@withContext null
            val json = context.contentResolver.openInputStream(newest.uri)?.use { input ->
                input.bufferedReader().use { it.readText() }
            } ?: return@withContext null
            name to json
        }

    companion object {
        const val BACKUP_PREFIX = "CheckIt_Backup_"
        const val BACKUP_EXTENSION = ".json"
    }
}
