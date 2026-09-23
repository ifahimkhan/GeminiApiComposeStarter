package com.fahim.geminiApiComposeStarter.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString

/**
 * Rich markdown and code block viewer for Gemini responses.
 * Parses and renders headings, paragraphs with bold markdown, and styled monospace code blocks with a Copy button.
 */
@Composable
fun MarkdownViewer(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val clipboardManager = LocalClipboardManager.current
    val sections = parseMarkdownSections(content)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (section in sections) {
            when (section) {
                is MarkdownSection.Heading -> {
                    Text(
                        text = section.text,
                        style = when (section.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                is MarkdownSection.Paragraph -> {
                    Text(
                        text = section.text.toBoldAnnotatedString(),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = textColor,
                    )
                }
                is MarkdownSection.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            // Code block header with language and Copy button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = section.language.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(section.code))
                                    },
                                    modifier = Modifier.padding(0.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(2.dp),
                                    )
                                }
                            }
                            // Horizontally scrollable code text
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = section.code,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class MarkdownSection {
    data class Heading(val text: String, val level: Int) : MarkdownSection()
    data class Paragraph(val text: String) : MarkdownSection()
    data class CodeBlock(val language: String, val code: String) : MarkdownSection()
}

private fun parseMarkdownSections(raw: String): List<MarkdownSection> {
    val sections = mutableListOf<MarkdownSection>()
    val codeBlockRegex = Regex("```([a-zA-Z0-9_-]*)\n([\\s\\S]*?)```")

    var lastIndex = 0
    for (match in codeBlockRegex.findAll(raw)) {
        val textBefore = raw.substring(lastIndex, match.range.first)
        if (textBefore.isNotBlank()) {
            parseTextBlocks(textBefore, sections)
        }
        val lang = match.groupValues[1].ifBlank { "code" }
        val code = match.groupValues[2].trimEnd()
        sections.add(MarkdownSection.CodeBlock(language = lang, code = code))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < raw.length) {
        val remainingText = raw.substring(lastIndex)
        if (remainingText.isNotBlank()) {
            parseTextBlocks(remainingText, sections)
        }
    }

    return sections
}

private fun parseTextBlocks(text: String, target: MutableList<MarkdownSection>) {
    val lines = text.lines()
    var currentParagraph = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("### ") -> {
                if (currentParagraph.isNotBlank()) {
                    target.add(MarkdownSection.Paragraph(currentParagraph.toString().trim()))
                    currentParagraph = StringBuilder()
                }
                target.add(MarkdownSection.Heading(trimmed.removePrefix("### ").trim(), 3))
            }
            trimmed.startsWith("## ") -> {
                if (currentParagraph.isNotBlank()) {
                    target.add(MarkdownSection.Paragraph(currentParagraph.toString().trim()))
                    currentParagraph = StringBuilder()
                }
                target.add(MarkdownSection.Heading(trimmed.removePrefix("## ").trim(), 2))
            }
            trimmed.startsWith("# ") -> {
                if (currentParagraph.isNotBlank()) {
                    target.add(MarkdownSection.Paragraph(currentParagraph.toString().trim()))
                    currentParagraph = StringBuilder()
                }
                target.add(MarkdownSection.Heading(trimmed.removePrefix("# ").trim(), 1))
            }
            trimmed.isBlank() -> {
                if (currentParagraph.isNotBlank()) {
                    target.add(MarkdownSection.Paragraph(currentParagraph.toString().trim()))
                    currentParagraph = StringBuilder()
                }
            }
            else -> {
                if (currentParagraph.isNotEmpty()) currentParagraph.append("\n")
                currentParagraph.append(line)
            }
        }
    }

    if (currentParagraph.isNotBlank()) {
        target.add(MarkdownSection.Paragraph(currentParagraph.toString().trim()))
    }
}
