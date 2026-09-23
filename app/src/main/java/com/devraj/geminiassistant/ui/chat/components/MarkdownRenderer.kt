package com.devraj.geminiassistant.ui.chat.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Header(val level: Int, val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Check for Fenced Code Block
        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim().ifEmpty { "code" }
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeBuilder.appendLine(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(language, codeBuilder.toString().trimEnd()))
            continue
        }

        // Check for Headers
        if (line.startsWith("### ")) {
            blocks.add(MarkdownBlock.Header(3, line.removePrefix("### ").trim()))
            i++
            continue
        } else if (line.startsWith("## ")) {
            blocks.add(MarkdownBlock.Header(2, line.removePrefix("## ").trim()))
            i++
            continue
        } else if (line.startsWith("# ")) {
            blocks.add(MarkdownBlock.Header(1, line.removePrefix("# ").trim()))
            i++
            continue
        }

        // Check for Bullet points
        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            val bulletText = line.trimStart().substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(bulletText))
            i++
            continue
        }

        // Standard text paragraph
        if (line.isNotBlank()) {
            val paragraphBuilder = StringBuilder(line)
            i++
            while (i < lines.size &&
                lines[i].isNotBlank() &&
                !lines[i].trimStart().startsWith("```") &&
                !lines[i].startsWith("#") &&
                !lines[i].trimStart().startsWith("- ") &&
                !lines[i].trimStart().startsWith("* ")
            ) {
                paragraphBuilder.append("\n").append(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.Paragraph(paragraphBuilder.toString()))
        } else {
            i++
        }
    }

    return blocks.ifEmpty { listOf(MarkdownBlock.Paragraph(rawText)) }
}

@Composable
fun MarkdownContent(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = formatInlineMarkdown(block.text),
                        style = style,
                        color = textColor
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatInlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(language = block.language, code = block.code)
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E1E2E), // Dark modern IDE slate theme
        shadowElevation = 2.dp
    ) {
        Column {
            // Header with language tag and copy code button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF282A36))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8BE9FD),
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = if (copied) Color(0xFF50FA7B) else Color(0xFFBD93F9),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Scrollable Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFF8F8F2),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("""(\*\*(.*?)\*\*)|(`(.*?)`)""")
        val matches = regex.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                append(text.substring(cursor, start))
            }

            val boldGroup = match.groups[2]
            val codeGroup = match.groups[4]

            if (boldGroup != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldGroup.value)
                }
            } else if (codeGroup != null) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x33888888),
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(" ${codeGroup.value} ")
                }
            }

            cursor = end
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
