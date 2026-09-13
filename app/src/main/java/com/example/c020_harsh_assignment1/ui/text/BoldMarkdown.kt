package com.example.c020_harsh_assignment1.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

fun String.toBoldAnnotatedString(): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0

        while (currentIndex < length) {
            val start = indexOf("**", currentIndex)

            if (start == -1) {
                append(substring(currentIndex))
                break
            }

            append(substring(currentIndex, start))

            val end = indexOf("**", start + 2)

            if (end == -1) {
                append(substring(start))
                break
            }

            pushStyle(
                SpanStyle(fontWeight = FontWeight.Bold)
            )
            append(substring(start + 2, end))
            pop()

            currentIndex = end + 2
        }
    }
}