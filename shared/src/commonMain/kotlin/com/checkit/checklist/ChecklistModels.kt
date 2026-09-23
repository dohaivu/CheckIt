package com.checkit.checklist

/**
 * A single markdown file inside the user-selected checklist folder.
 *
 * @param uri opaque platform URI of the file (SAF document URI on Android, path elsewhere).
 * @param name file name including the `.md` extension, used for display and sorting.
 */
data class ChecklistDocument(
    val uri: String,
    val name: String
) {
    /** File name without the trailing `.md` extension, for display. */
    val title: String
        get() = name.removeSuffix(".md").removeSuffix(".MD").ifBlank { name }
}

/** Reads markdown files from the user-selected checklist folder. */
interface ChecklistStorage {
    /** Lists `.md` files in [folderUri]. Returns an empty list when the folder is unreadable. */
    suspend fun listMarkdownFiles(folderUri: String): List<ChecklistDocument>

    /** Reads the full markdown text of the file at [documentUri]. */
    suspend fun readMarkdownFile(documentUri: String): String
}

/** Fallback for platforms without local folder access: always empty / failing. */
class NoOpChecklistStorage : ChecklistStorage {
    override suspend fun listMarkdownFiles(folderUri: String): List<ChecklistDocument> = emptyList()

    override suspend fun readMarkdownFile(documentUri: String): String =
        error("Checklist folders are not supported on this platform")
}

/** Keeps only `.md` files (case-insensitive) and sorts them by name. */
fun filterMarkdownDocuments(names: List<Pair<String, String>>): List<ChecklistDocument> =
    names
        .filter { (_, name) -> name.endsWith(".md", ignoreCase = true) }
        .map { (uri, name) -> ChecklistDocument(uri = uri, name = name) }
        .sortedBy { it.name.lowercase() }
