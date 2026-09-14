package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.geminiApiComposeStarter.ui.text.*
import kotlinx.coroutines.delay

@Composable
fun ResponseContent(text: String, highlightQuery: String? = null) {
    val blocks = remember(text) { parseResponseBlocks(text) }
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            blocks.forEach { block ->
                if (block.language == null) {
                    // Render rich markdown prose (headings, bullet lists, blockquotes, tables, inline bold & code)
                    val elements = remember(block.text) { parseMarkdownParagraphs(block.text) }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        elements.forEach { element ->
                            when (element) {
                                is MarkdownElement.Heading -> {
                                    val style = when (element.level) {
                                        1 -> MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        2 -> MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                        3 -> MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                        else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        text = element.text.toFormattedAnnotatedString(highlightQuery),
                                        style = style,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    )
                                }
                                is MarkdownElement.ListItem -> {
                                    Row(
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Text(
                                            text = element.prefix,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            ),
                                            modifier = Modifier.width(24.dp),
                                        )
                                        Text(
                                            text = element.text.toFormattedAnnotatedString(highlightQuery),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                }
                                is MarkdownElement.BlockQuote -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF10A37F)),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = element.text.toFormattedAnnotatedString(highlightQuery),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                        )
                                    }
                                }
                                is MarkdownElement.Divider -> {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                                is MarkdownElement.Table -> {
                                    MarkdownTable(element, highlightQuery)
                                }
                                is MarkdownElement.Paragraph -> {
                                    Text(
                                        text = element.text.toFormattedAnnotatedString(highlightQuery),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Code block rendering
                    val clipboard = LocalClipboardManager.current
                    var copied by remember(block.text) { mutableStateOf(false) }
                    LaunchedEffect(copied) { if (copied) { delay(2000); copied = false } }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = block.language.ifBlank { "code" },
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                )
                                TextButton(onClick = { clipboard.setText(AnnotatedString(block.text)); copied = true }) {
                                    Text(
                                        if (copied) "Copied!" else "Copy",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary),
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            Text(
                                text = block.text,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(16.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(table: MarkdownElement.Table, highlightQuery: String? = null) {
    val scrollState = rememberScrollState()
    val colCount = remember(table) {
        maxOf(table.headers.size, table.rows.maxOfOrNull { it.size } ?: 0)
    }
    // Calculate optimal column widths per column index to prevent cell text squishing & clipping
    val colWidths = remember(table, colCount) {
        List(colCount) { colIdx ->
            val maxLen = maxOf(
                table.headers.getOrNull(colIdx)?.length ?: 0,
                table.rows.maxOfOrNull { it.getOrNull(colIdx)?.length ?: 0 } ?: 0
            )
            (maxLen * 9.5f).toInt().dp.coerceIn(140.dp, 340.dp)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(vertical = 2.dp)
        ) {
            Column {
                // ── Header Row ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (colIdx in 0 until colCount) {
                        val headerText = table.headers.getOrNull(colIdx) ?: ""
                        Box(
                            modifier = Modifier
                                .width(colWidths[colIdx])
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = headerText.toFormattedAnnotatedString(highlightQuery),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.5.sp
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // ── Data Rows ───────────────────────────────────────────────
                table.rows.forEachIndexed { rowIndex, row ->
                    val rowBg = if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh
                    Row(
                        modifier = Modifier
                            .background(rowBg)
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (colIdx in 0 until colCount) {
                            val cellText = row.getOrNull(colIdx) ?: ""
                            Box(
                                modifier = Modifier
                                    .width(colWidths[colIdx])
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = cellText.toFormattedAnnotatedString(highlightQuery),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }
                    if (rowIndex < table.rows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
