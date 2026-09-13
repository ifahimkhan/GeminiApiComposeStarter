package com.example.c031_geminiapicompose.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Converts simple Markdown bold syntax (`**text**`) into Compose AnnotatedString.
 */
fun String.toBoldAnnotatedString(): AnnotatedString {
    val regex = Regex("""\*\*(.*?)\*\*""")
    var currentIndex = 0
    return buildAnnotatedString {
        for (match in regex.findAll(this@toBoldAnnotatedString)) {
            val range = match.range
            if (range.first > currentIndex) {
                append(this@toBoldAnnotatedString.substring(currentIndex, range.first))
            }
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            currentIndex = range.last + 1
        }
        if (currentIndex < this@toBoldAnnotatedString.length) {
            append(this@toBoldAnnotatedString.substring(currentIndex))
        }
    }
}
