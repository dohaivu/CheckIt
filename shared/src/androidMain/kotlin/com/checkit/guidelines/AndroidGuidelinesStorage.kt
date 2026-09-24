package com.checkit.guidelines

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android SAF-backed [GuidelinesStorage].
 *
 * The folder URI comes from `ActivityResultContracts.OpenDocumentTree()` and is
 * persisted in [com.checkit.data.AppDataStore]; read access requires the
 * persistable URI permission taken at pick time (same pattern as backup).
 */
class AndroidGuidelinesStorage(
    private val context: Context
) : GuidelinesStorage {
    override suspend fun listMarkdownFiles(folderUri: String): List<GuidelinesDocument> =
        withContext(Dispatchers.IO) {
            val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                ?: return@withContext emptyList()
            val entries = folder.listFiles()
                .filter { it.isFile }
                .mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    file.uri.toString() to name
                }
            filterMarkdownDocuments(entries)
        }

    override suspend fun readMarkdownFile(documentUri: String): String =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(Uri.parse(documentUri))?.use { input ->
                input.bufferedReader().use { it.readText() }
            } ?: error("Cannot read guidelines file")
        }
}
