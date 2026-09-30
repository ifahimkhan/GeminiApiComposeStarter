package com.fahim.geminiApiComposeStarter.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

/**
 * Converts a Gemini markdown response to an [AnnotatedString] with:
 *  - **bold** via `**text**`
 *  - *italic* via `*text*` (single asterisk, not already consumed by bold)
 *  - `code` via backtick spans
 *  - # H1 / ## H2 / ### H3 headings (line-level)
 *  - - or * bullet list items (line-level, prepends "•")
 *
 * Limitations: tables, fenced code blocks, nested lists, and links are
 * rendered as plain text. This covers the majority of Gemini's responses
 * without requiring a full Markdown library dependency.
 */
fun String.toMarkdownAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val lines = this@toMarkdownAnnotatedString.lines()
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()

        // Heading detection: # H1 / ## H2 / ### H3
        val headingMatch = Regex("""^(#{1,3})\s+(.+)$""").matchEntire(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val headingText = headingMatch.groupValues[2]
            val fontSize = when (level) {
                1 -> 1.25.em
                2 -> 1.1.em
                else -> 1.0.em
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
                appendInlineMarkdown(headingText)
            }
        } else {
            // Bullet list: lines starting with "- " or "* "
            val bulletMatch = Regex("""^(\s*)[*\-]\s+(.+)$""").matchEntire(line)
            if (bulletMatch != null) {
                val content = bulletMatch.groupValues[2]
                append("• ")
                appendInlineMarkdown(content)
            } else {
                appendInlineMarkdown(line)
            }
        }

        if (index < lines.lastIndex) {
            append("\n")
        }
    }
}

/**
 * Appends inline markdown spans (**bold**, *italic*, `code`) to the current
 * [AnnotatedString.Builder] context.
 */
private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    // Tokenise: find **bold**, *italic*, `code` spans in order of appearance.
    val tokenPattern = Regex("""\*\*(.+?)\*\*|\*([^*]+?)\*|`([^`]+?)`""")
    var cursor = 0
    for (match in tokenPattern.findAll(text)) {
        // Append any plain text before this match
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        when {
            match.groupValues[1].isNotEmpty() -> {
                // **bold**
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            }
            match.groupValues[2].isNotEmpty() -> {
                // *italic*
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[2])
                }
            }
            match.groupValues[3].isNotEmpty() -> {
                // `code`
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                    )
                ) {
                    append(match.groupValues[3])
                }
            }
        }
        cursor = match.range.last + 1
    }
    // Append any remaining plain text after the last match
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}

/**
 * Legacy alias kept for callers that previously used [toBoldAnnotatedString].
 * Delegates to the richer [toMarkdownAnnotatedString].
 */
fun String.toBoldAnnotatedString(): AnnotatedString = toMarkdownAnnotatedString()
