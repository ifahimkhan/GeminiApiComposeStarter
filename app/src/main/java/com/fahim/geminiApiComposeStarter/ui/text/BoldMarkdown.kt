
package com.fahim.geminiApiComposeStarter.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val BOLD_PATTERN = Regex("""\*\*(.*?)\*\*""")


fun String.toBoldAnnotatedString(): AnnotatedString {
    val cleanedText = this.lines().joinToString("\n") { line ->
        line
            .replace(Regex("^\\s{0,3}#{1,6}\\s+"), "")
            .replace(Regex("^\\s*[-*+]\\s+"), "• ")
            .replace(Regex("^\\s*\\d+\\.\\s+"), "$0")
    }

    return buildAnnotatedString {
        var lastIndex = 0

        for (match in BOLD_PATTERN.findAll(cleanedText)) {
            append(cleanedText.substring(lastIndex, match.range.first))

            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < cleanedText.length) {
            append(cleanedText.substring(lastIndex))
        }
    }
}
