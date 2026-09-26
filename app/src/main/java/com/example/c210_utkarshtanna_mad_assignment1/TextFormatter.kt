package com.example.c210_utkarshtanna_mad_assignment1

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

object TextFormatter {
    private val BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*")

    fun formatToAnnotatedString(text: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            val matcher = BOLD_PATTERN.matcher(text)
            var lastIndex = 0

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                if (start > lastIndex) {
                    append(text.substring(lastIndex, start))
                }

                val boldText = matcher.group(1) ?: ""
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldText)
                }

                lastIndex = end
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
