package com.checkit.domain

data class ActiveTagToken(
    val query: String,
    val startIndex: Int,
    val endIndex: Int
)

object TagParser {
    private val tagRegex = Regex("""(^|[^\p{L}\p{N}_-])#([\p{L}\p{N}_-]+)""")
    private val allowedTagChar = Regex("""[\p{L}\p{N}_-]""")

    fun parse(note: String): List<String> {
        return tagRegex.findAll(note)
            .map { it.groupValues[2] }
            .map(::normalize)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    fun normalize(tag: String): String =
        tag.trim().removePrefix("#").trim().lowercase()

    fun activeToken(text: String, cursorIndex: Int): ActiveTagToken? {
        val safeCursor = cursorIndex.coerceIn(0, text.length)
        val hashIndex = text.lastIndexOf('#', startIndex = (safeCursor - 1).coerceAtLeast(0))
        if (hashIndex < 0) return null
        if (hashIndex >= safeCursor) return null
        if (hashIndex > 0 && !text[hashIndex - 1].isWhitespace()) return null

        val query = text.substring(hashIndex + 1, safeCursor)
        if (query.any { !allowedTagChar.matches(it.toString()) }) return null

        val tokenEnd = generateSequence(safeCursor) { index ->
            if (index < text.length && allowedTagChar.matches(text[index].toString())) index + 1 else null
        }.last()

        return ActiveTagToken(query = normalize(query), startIndex = hashIndex, endIndex = tokenEnd)
    }

    fun replaceActiveToken(text: String, token: ActiveTagToken, tag: String): String {
        val replacement = "#${normalize(tag)}"
        return buildString {
            append(text.substring(0, token.startIndex))
            append(replacement)
            append(" ")
            append(text.substring(token.endIndex))
        }.replace(Regex("""\s{2,}"""), " ")
    }

    fun renameTagInNote(note: String, oldTag: String, newTag: String): String {
        val oldNormalized = normalize(oldTag)
        val newNormalized = normalize(newTag)
        if (oldNormalized.isBlank() || newNormalized.isBlank() || oldNormalized == newNormalized) return note

        return tagRegex.replace(note) { match ->
            if (normalize(match.groupValues[2]) == oldNormalized) {
                "${match.groupValues[1]}#$newNormalized"
            } else {
                match.value
            }
        }
    }

    fun removeTagFromNote(note: String, tag: String): String {
        val normalized = normalize(tag)
        if (normalized.isBlank()) return note

        return tagRegex.replace(note) { match ->
            if (normalize(match.groupValues[2]) == normalized) {
                match.groupValues[1]
            } else {
                match.value
            }
        }
            .replace(Regex("""[ \t]{2,}"""), " ")
            .replace(Regex("""\s+([,.!?;:])"""), "$1")
            .trim()
    }
}
