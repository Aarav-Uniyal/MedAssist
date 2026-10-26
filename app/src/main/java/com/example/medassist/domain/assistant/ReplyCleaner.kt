package com.example.medassist.domain.assistant

/** Removes Markdown the model sometimes adds anyway, so replies read well as plain text and in TalkBack. */
object ReplyCleaner {
    private val heading = Regex("^\\s{0,3}#{1,6}\\s+", RegexOption.MULTILINE)
    private val bullet = Regex("^(\\s*)[*+]\\s+", RegexOption.MULTILINE)
    private val emphasis = Regex("(\\*\\*|__)(.+?)\\1")
    private val blankLines = Regex("\n{3,}")

    fun clean(text: String): String = text
        .replace(heading, "")
        .replace(emphasis, "$2")
        .replace(bullet, "$1- ")
        .replace(blankLines, "\n\n")
        .trim()
}
