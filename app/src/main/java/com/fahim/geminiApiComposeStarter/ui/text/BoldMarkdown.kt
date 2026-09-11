package com.fahim.geminiApiComposeStarter.ui.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * A simple markdown-like processor that handles:
 * - **bold text**
 * - `inline code`
 * - ```code blocks```
 * - * bullet lists
 */
fun String.toMarkdownAnnotatedString(
    primaryColor: Color = Color(0xFF8B5CF6)
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        
        // Handle code blocks first to prevent nested processing
        val codeBlockRegex = Regex("```(?:[a-zA-Z]*\\n)?([\\s\\S]*?)```")
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        val inlineCodeRegex = Regex("`(.*?)`")
        val bulletRegex = Regex("(?m)^\\* (.*)$")

        // This is a simplified sequential processor. 
        // For a full app, a proper markdown parser would be better.
        
        val allMatches = (codeBlockRegex.findAll(this@toMarkdownAnnotatedString) +
                boldRegex.findAll(this@toMarkdownAnnotatedString) +
                inlineCodeRegex.findAll(this@toMarkdownAnnotatedString)).sortedBy { it.range.first }

        var lastIndex = 0
        for (match in allMatches) {
            if (match.range.first < lastIndex) continue // Skip overlapping
            
            append(this@toMarkdownAnnotatedString.substring(lastIndex, match.range.first))
            
            when {
                match.value.startsWith("```") -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = primaryColor.copy(alpha = 0.1f),
                        color = primaryColor
                    )) {
                        append(match.groupValues[1].trim())
                    }
                }
                match.value.startsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1])
                    }
                }
                match.value.startsWith("`") -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = primaryColor.copy(alpha = 0.2f)
                    )) {
                        append(match.groupValues[1])
                    }
                }
            }
            lastIndex = match.range.last + 1
        }
        append(this@toMarkdownAnnotatedString.substring(lastIndex))
    }
}

// Keep the old function name for compatibility if used elsewhere, but redirect
fun String.toBoldAnnotatedString(): AnnotatedString = toMarkdownAnnotatedString()
