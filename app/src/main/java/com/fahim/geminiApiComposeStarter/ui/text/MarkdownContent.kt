package com.fahim.geminiApiComposeStarter.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface MarkdownBlock {
    data class TextBlock(val content: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String = "") : MarkdownBlock
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

/** Parses raw markdown text containing regular text, code fences, and markdown tables. */
fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val lines = rawText.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Code Block detection
        if (line.trim().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size && lines[i].trim().startsWith("```")) {
                i++ // skip closing fence
            }
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language))
            continue
        }

        // 2. Markdown Table detection
        if (isTableLine(line) && i + 1 < lines.size && isTableSeparatorLine(lines[i + 1])) {
            val headers = parseTableRow(line)
            i += 2 // skip header and separator
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && isTableLine(lines[i])) {
                val row = parseTableRow(lines[i])
                if (row.isNotEmpty()) {
                    rows.add(row)
                }
                i++
            }
            blocks.add(MarkdownBlock.TableBlock(headers, rows))
            continue
        }

        // 3. Regular text accumulator
        val textLines = mutableListOf<String>()
        while (i < lines.size && !lines[i].trim().startsWith("```") && !(isTableLine(lines[i]) && i + 1 < lines.size && isTableSeparatorLine(lines[i + 1]))) {
            textLines.add(lines[i])
            i++
        }
        val text = textLines.joinToString("\n").trim()
        if (text.isNotEmpty()) {
            blocks.add(MarkdownBlock.TextBlock(text))
        }
    }

    return blocks
}

private fun isTableLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2
}

private fun isTableSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) return false
    val inner = trimmed.substring(1, trimmed.length - 1)
    val cells = inner.split("|")
    return cells.isNotEmpty() && cells.all { cell ->
        val c = cell.trim()
        c.isNotEmpty() && c.all { it == '-' || it == ':' }
    }
}

private fun parseTableRow(line: String): List<String> {
    val trimmed = line.trim()
    val inner = if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
        trimmed.substring(1, trimmed.length - 1)
    } else {
        trimmed
    }
    return inner.split("|").map { it.trim() }
}

@Composable
fun MarkdownMessageView(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = parseMarkdownBlocks(text)
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.TextBlock -> {
                    Text(
                        text = block.content.toBoldAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                is MarkdownBlock.TableBlock -> {
                    MarkdownTableView(table = block)
                }
                is MarkdownBlock.CodeBlock -> {
                    MarkdownCodeView(code = block.code, language = block.language)
                }
            }
            if (index < blocks.size - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun MarkdownTableView(table: MarkdownBlock.TableBlock) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    .padding(vertical = 4.dp),
            ) {
                table.headers.forEach { header ->
                    Box(
                        modifier = Modifier
                            .widthIn(min = 100.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = header.toBoldAnnotatedString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            // Data Rows
            table.rows.forEachIndexed { rowIndex, row ->
                val rowBackground = if (rowIndex % 2 == 1) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
                Row(
                    modifier = Modifier
                        .background(rowBackground)
                        .padding(vertical = 2.dp),
                ) {
                    table.headers.indices.forEach { colIndex ->
                        val cellText = row.getOrNull(colIndex) ?: ""
                        Box(
                            modifier = Modifier
                                .widthIn(min = 100.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = cellText.toBoldAnnotatedString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                if (rowIndex < table.rows.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
            }
        }
    }
}

@Composable
fun MarkdownCodeView(code: String, language: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            if (language.isNotBlank()) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                text = code,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
