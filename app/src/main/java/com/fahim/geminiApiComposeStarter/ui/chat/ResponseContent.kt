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
fun ResponseContent(text: String) {
    val blocks = remember(text) { parseResponseBlocks(text) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        blocks.forEach { block ->
            if (block.language == null) {
                // Render rich markdown prose (headings, bullet lists, blockquotes, inline bold & code)
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
                                SelectionContainer {
                                    Text(
                                        text = element.text.toFormattedAnnotatedString(),
                                        style = style,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    )
                                }
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
                                    SelectionContainer {
                                        Text(
                                            text = element.text.toFormattedAnnotatedString(),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
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
                                    SelectionContainer {
                                        Text(
                                            text = element.text.toFormattedAnnotatedString(),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                        )
                                    }
                                }
                            }
                            is MarkdownElement.Divider -> {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            is MarkdownElement.Paragraph -> {
                                SelectionContainer {
                                    Text(
                                        text = element.text.toFormattedAnnotatedString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
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
                    color = Color(0xFF1E1E1E), // ChatGPT dark code box
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D2D))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = block.language.ifBlank { "code" },
                                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFECECEC)),
                            )
                            TextButton(onClick = { clipboard.setText(AnnotatedString(block.text)); copied = true }) {
                                Text(
                                    if (copied) "Copied!" else "Copy",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFECECEC)),
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF3E3E3E), thickness = 0.5.dp)
                        SelectionContainer {
                            Text(
                                text = block.text,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(16.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD4D4D4)),
                            )
                        }
                    }
                }
            }
        }
    }
}
