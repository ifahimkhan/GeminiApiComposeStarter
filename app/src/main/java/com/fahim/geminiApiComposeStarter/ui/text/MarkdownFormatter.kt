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

@Immutable
sealed class MarkdownElement {
    @Immutable data class Heading(val level: Int, val text: String) : MarkdownElement()
    @Immutable data class ListItem(val isNumbered: Boolean, val prefix: String, val text: String) : MarkdownElement()
    @Immutable data class BlockQuote(val text: String) : MarkdownElement()
    @Immutable object Divider : MarkdownElement()
    @Immutable data class Paragraph(val text: String) : MarkdownElement()
}

private val markdownCache = LruCache<String, List<MarkdownElement>>(200)
private val annotatedStringCache = LruCache<String, AnnotatedString>(300)
private val INLINE_REGEX = Regex("""(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|_[^_]+_)""")

/** Parses non-code prose into rich markdown elements (headings, lists, blockquotes, paragraphs). Cached for high performance. */
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

    for (line in lines) {
        val trimmed = line.trim()
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
    }
    flushParagraph()
    markdownCache.put(prose, elements)
    return elements
}

/** Parses inline markdown tags: **bold**, *italic*, and `inline code`. Cached for fast recompositions. */
fun String.toFormattedAnnotatedString(): AnnotatedString {
    annotatedStringCache.get(this)?.let { return it }

    val formatted = buildAnnotatedString {
        var index = 0
        val text = this@toFormattedAnnotatedString
        val matches = INLINE_REGEX.findAll(text)

        for (match in matches) {
            if (match.range.first > index) {
                append(text.substring(index, match.range.first))
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
                        append(raw)
                    }
                }
                (value.startsWith("*") && value.endsWith("*")) || (value.startsWith("_") && value.endsWith("_")) -> {
                    val raw = value.substring(1, value.length - 1)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(raw)
                    }
                }
                else -> append(value)
            }
            index = match.range.last + 1
        }

        if (index < text.length) {
            append(text.substring(index))
        }
    }
    annotatedStringCache.put(this, formatted)
    return formatted
}
