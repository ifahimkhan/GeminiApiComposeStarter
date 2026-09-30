package com.fahim.geminiApiComposeStarter.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.geminiApiComposeStarter.R

private val BOLD_PATTERN = Regex("""\*\*(.*?)\*\*""")

/** Port of the reference app's TextFormatter: renders `**bold**` spans, drops the markers. */
fun String.toBoldAnnotatedString(): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    for (match in BOLD_PATTERN.findAll(this@toBoldAnnotatedString)) {
        append(this@toBoldAnnotatedString.substring(lastIndex, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < this@toBoldAnnotatedString.length) {
        append(this@toBoldAnnotatedString.substring(lastIndex))
    }
}

/** Rich Markdown Renderer Composable that handles headers, paragraphs, code blocks, lists, bold text, and inline code. */
@Composable
fun FormattedMarkdownMessage(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val clipboardManager = LocalClipboardManager.current
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockItem(
                        code = block.code,
                        language = block.language,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(block.code))
                        },
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    ParagraphItem(
                        text = block.content,
                        textColor = textColor,
                    )
                }

                is MarkdownBlock.Heading -> {
                    HeadingItem(
                        text = block.content,
                        level = block.level,
                        textColor = textColor,
                    )
                }

                is MarkdownBlock.ListItem -> {
                    ListItemBlock(
                        text = block.content,
                        textColor = textColor,
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Paragraph(val content: String) : MarkdownBlock
    data class Heading(val content: String, val level: Int) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String = "") : MarkdownBlock
    data class ListItem(val content: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()

    var inCodeBlock = false
    var currentCodeLanguage = ""
    val codeBuffer = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(
                    MarkdownBlock.CodeBlock(
                        code = codeBuffer.toString().trimEnd(),
                        language = currentCodeLanguage,
                    )
                )
                codeBuffer.clear()
                inCodeBlock = false
                currentCodeLanguage = ""
            } else {
                inCodeBlock = true
                currentCodeLanguage = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            if (codeBuffer.isNotEmpty()) codeBuffer.append("\n")
            codeBuffer.append(line)
            continue
        }

        if (trimmed.startsWith("# ")) {
            blocks.add(MarkdownBlock.Heading(trimmed.removePrefix("# ").trim(), 1))
            continue
        }
        if (trimmed.startsWith("## ")) {
            blocks.add(MarkdownBlock.Heading(trimmed.removePrefix("## ").trim(), 2))
            continue
        }
        if (trimmed.startsWith("### ")) {
            blocks.add(MarkdownBlock.Heading(trimmed.removePrefix("### ").trim(), 3))
            continue
        }

        if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
            val content = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.ListItem(content))
            continue
        }

        if (trimmed.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(code = codeBuffer.toString(), language = currentCodeLanguage))
    }

    return blocks
}

@Composable
private fun ParagraphItem(text: String, textColor: Color) {
    val parsedText = remember(text) { parseRichText(text) }
    Text(
        text = parsedText,
        color = textColor,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun HeadingItem(text: String, level: Int, textColor: Color) {
    val (fontSize, fontWeight) = when (level) {
        1 -> 20.sp to FontWeight.Bold
        2 -> 18.sp to FontWeight.SemiBold
        else -> 16.sp to FontWeight.SemiBold
    }
    val parsedText = remember(text) { parseRichText(text) }

    Text(
        text = parsedText,
        color = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ListItemBlock(text: String, textColor: Color) {
    val parsedText = remember(text) { parseRichText(text) }
    Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
        Text(
            text = "• ",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text(
            text = parsedText,
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun CodeBlockItem(
    code: String,
    language: String,
    onCopy: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E2E),
        contentColor = Color(0xFFCDD6F4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181825))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.ifEmpty { "code" },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA6ADC8),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.copy_message),
                        tint = Color(0xFFA6ADC8),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFCDD6F4),
                )
            }
        }
    }
}

private fun parseRichText(text: String): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    val matches = BOLD_PATTERN.findAll(text).toList()

    for (match in matches) {
        append(text.substring(lastIndex, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}
