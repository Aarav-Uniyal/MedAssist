package com.example.medassist.domain.vault

/**
 * Small, deterministic rules for preparing PDF text before it is sent to the report analyzer.
 * This keeps whitespace handling and request-size limits independent of Android/PDFBox.
 */
object ReportTextPolicy {
    const val MAX_ANALYSIS_CHARS = 22_000
    const val MIN_ANALYSIS_CHARS = 20

    data class PreparedText(val text: String, val truncated: Boolean)

    /** Returns null when the PDF has too little selectable text to analyze reliably. */
    fun prepare(raw: String): PreparedText? {
        val normalized = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u000C', '\n')
            .replace('\u0000', ' ')
            .replace(Regex("[\\t\\u00A0 ]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        if (normalized.length < MIN_ANALYSIS_CHARS) return null

        var text = normalized.take(MAX_ANALYSIS_CHARS)
        // Don't leave half of a UTF-16 surrogate pair at the end of a truncated request.
        if (text.isNotEmpty() && text.last().isHighSurrogate()) text = text.dropLast(1)
        if (text.length < MIN_ANALYSIS_CHARS) return null

        return PreparedText(
            text = text,
            truncated = normalized.length > text.length,
        )
    }
}
