package com.fahim.geminiApiComposeStarter.ui.text

import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import java.util.Locale

@Immutable
sealed class MarkdownElement {
    @Immutable data class Heading(val level: Int, val text: String) : MarkdownElement()
    @Immutable data class ListItem(val isNumbered: Boolean, val prefix: String, val text: String) : MarkdownElement()
    @Immutable data class BlockQuote(val text: String) : MarkdownElement()
    @Immutable object Divider : MarkdownElement()
    @Immutable data class Paragraph(val text: String) : MarkdownElement()
    @Immutable data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownElement()
}

private val markdownCache = LruCache<String, List<MarkdownElement>>(200)
private val annotatedStringCache = LruCache<String, AnnotatedString>(300)
private val INLINE_REGEX = Regex("""(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|_[^_]+_)""")

private fun isTableLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.contains("|")) return false
    val pipeCount = trimmed.count { it == '|' }
    return pipeCount >= 2 || trimmed.startsWith("|") || trimmed.endsWith("|")
}

private fun isTableSeparator(line: String): Boolean {
    val trimmed = line.trim()
    val stripped = trimmed.replace("|", "").replace("-", "").replace(":", "").replace(" ", "")
    return stripped.isEmpty() && trimmed.contains("-")
}

private fun parseTableLines(lines: List<String>): MarkdownElement.Table? {
    if (lines.isEmpty()) return null
    val parsedRows = mutableListOf<List<String>>()
    for (line in lines) {
        if (isTableSeparator(line)) continue
        val parts = line.split("|")
        val cells = parts
            .map { it.trim() }
            .filterIndexed { idx, cell ->
                !(idx == 0 && cell.isEmpty()) && !(idx == parts.lastIndex && cell.isEmpty())
            }
        if (cells.isNotEmpty()) {
            parsedRows.add(cells)
        }
    }
    if (parsedRows.isEmpty()) return null
    val headers = parsedRows.first()
    val dataRows = if (parsedRows.size > 1) parsedRows.subList(1, parsedRows.size) else emptyList()
    return MarkdownElement.Table(headers, dataRows)
}

/** Parses non-code prose into rich markdown elements (headings, lists, blockquotes, tables, paragraphs). Cached for high performance. */
fun parseMarkdownParagraphs(prose: String): List<MarkdownElement> {
    markdownCache.get(prose)?.let { return it }

    val elements = mutableListOf<MarkdownElement>()
    val lines = prose.lines()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            val text = paragraphBuffer.toString().trim()
            if (text.isNotEmpty()) {
                elements.add(MarkdownElement.Paragraph(text))
            }
            paragraphBuffer.clear()
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (isTableLine(trimmed)) {
            flushParagraph()
            val tableLines = mutableListOf<String>()
            while (i < lines.size && isTableLine(lines[i].trim())) {
                tableLines.add(lines[i].trim())
                i++
            }
            val table = parseTableLines(tableLines)
            if (table != null) {
                elements.add(table)
            } else {
                tableLines.forEach { elements.add(MarkdownElement.Paragraph(it)) }
            }
            continue
        }

        when {
            trimmed.startsWith("# ") -> {
                flushParagraph()
                elements.add(MarkdownElement.Heading(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                elements.add(MarkdownElement.Heading(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("### ") -> {
                flushParagraph()
                elements.add(MarkdownElement.Heading(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("#### ") -> {
                flushParagraph()
                elements.add(MarkdownElement.Heading(4, trimmed.removePrefix("#### ").trim()))
            }
            trimmed == "---" || trimmed == "***" -> {
                flushParagraph()
                elements.add(MarkdownElement.Divider)
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                elements.add(MarkdownElement.BlockQuote(trimmed.removePrefix("> ").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                flushParagraph()
                elements.add(MarkdownElement.ListItem(false, "•", trimmed.substring(2).trim()))
            }
            trimmed.matches(Regex("""^\d+\.\s.*""")) -> {
                flushParagraph()
                val dotIdx = trimmed.indexOf('.')
                val prefix = trimmed.substring(0, dotIdx + 1)
                val body = trimmed.substring(dotIdx + 1).trim()
                elements.add(MarkdownElement.ListItem(true, prefix, body))
            }
            trimmed.isEmpty() -> {
                flushParagraph()
            }
            else -> {
                if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append(" ")
                paragraphBuffer.append(trimmed)
            }
        }
        i++
    }
    flushParagraph()
    markdownCache.put(prose, elements)
    return elements
}

/** Parses inline markdown tags: **bold**, *italic*, `inline code`, and highlights matched search query terms. Cached for fast recompositions. */
fun String.toFormattedAnnotatedString(highlightQuery: String? = null): AnnotatedString {
    val cacheKey = if (highlightQuery.isNullOrBlank()) this else "$this::HL::$highlightQuery"
    annotatedStringCache.get(cacheKey)?.let { return it }

    val formatted = buildAnnotatedString {
        var index = 0
        val text = this@toFormattedAnnotatedString
        val matches = INLINE_REGEX.findAll(text)

        for (match in matches) {
            if (match.range.first > index) {
                appendWithHighlight(text.substring(index, match.range.first), highlightQuery)
            }
            val value = match.value
            when {
                value.startsWith("`") && value.endsWith("`") -> {
                    val raw = value.substring(1, value.length - 1)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF2D2D2D),
                            color = Color(0xFFE2E2E2),
                            fontSize = 13.5.sp
                        )
                    ) {
                        append(" $raw ")
                    }
                }
                value.startsWith("**") && value.endsWith("**") -> {
                    val raw = value.substring(2, value.length - 2)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendWithHighlight(raw, highlightQuery)
                    }
                }
                (value.startsWith("*") && value.endsWith("*")) || (value.startsWith("_") && value.endsWith("_")) -> {
                    val raw = value.substring(1, value.length - 1)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendWithHighlight(raw, highlightQuery)
                    }
                }
                else -> appendWithHighlight(value, highlightQuery)
            }
            index = match.range.last + 1
        }

        if (index < text.length) {
            appendWithHighlight(text.substring(index), highlightQuery)
        }
    }
    annotatedStringCache.put(cacheKey, formatted)
    return formatted
}

private fun AnnotatedString.Builder.appendWithHighlight(plainText: String, query: String?) {
    if (query.isNullOrBlank()) {
        append(plainText)
        return
    }
    var start = 0
    val lowerText = plainText.lowercase(Locale.getDefault())
    val lowerQuery = query.lowercase(Locale.getDefault())
    var idx = lowerText.indexOf(lowerQuery, start)

    while (idx != -1) {
        if (idx > start) {
            append(plainText.substring(start, idx))
        }
        withStyle(
            SpanStyle(
                background = Color(0xFFFFC107), // Bright Amber / Yellow highlight badge
                color = Color(0xFF000000),       // High-contrast dark text
                fontWeight = FontWeight.Bold
            )
        ) {
            append(plainText.substring(idx, idx + query.length))
        }
        start = idx + query.length
        idx = lowerText.indexOf(lowerQuery, start)
    }
    if (start < plainText.length) {
        append(plainText.substring(start))
    }
}
