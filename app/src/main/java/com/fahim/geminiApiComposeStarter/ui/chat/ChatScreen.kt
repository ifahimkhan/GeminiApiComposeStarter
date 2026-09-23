package com.fahim.geminiApiComposeStarter.ui.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.CardSurfaceWhite
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.PrimaryBlue
import com.fahim.geminiApiComposeStarter.ui.theme.SurfaceVariantLight
import com.fahim.geminiApiComposeStarter.ui.theme.TimestampPillBg
import com.fahim.geminiApiComposeStarter.ui.theme.TimestampPillText
import com.fahim.geminiApiComposeStarter.ui.theme.UserBubbleBlue
import com.fahim.geminiApiComposeStarter.ui.theme.UserBubbleTime

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = { viewModel.onSend() },
        onSuggestionClick = { suggestion -> viewModel.onSend(suggestion) },
        onClearChat = viewModel::onClearChat,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onClearChat: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(value = false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gemini Live Chat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                onClick = {
                                    showMenu = false
                                    onClearChat()
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp),
                        shape = CircleShape,
                        color = PrimaryBlue
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Timestamp Pill
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DatePill()
                    }
                }

                // Messages list
                items(state.messages, key = { it.id }) { message ->
                    if (message.isFromUser) {
                        UserMessageBubble(message = message)
                    } else {
                        ModelResponseBubble(message = message)
                    }
                }

                // Loading / Thinking state
                if (state.isLoading) {
                    item {
                        ThinkingBubble()
                    }
                }
            }

            // Quick Suggestions Row
            SuggestionsRow(onSuggestionClick = onSuggestionClick)

            // Bottom Floating Input Bar
            BottomInputBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend
            )
        }
    }
}

@Composable
private fun DatePill() {
    Surface(
        color = TimestampPillBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = TimestampPillText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Today, 10:24 AM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TimestampPillText
            )
        }
    }
}

@Composable
private fun UserMessageBubble(message: ChatMessageEntity) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            color = UserBubbleBlue,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 4.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.formattedTime.ifEmpty { "10:24 AM" },
                        color = UserBubbleTime,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Sent",
                        tint = UserBubbleTime,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun ModelResponseBubble(message: ChatMessageEntity) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
    ) {
        // Model Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = "Gemini",
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Gemini",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "2.0 Flash",
                fontSize = 13.sp,
                color = Color(0xFF5C6068)
            )
        }

        // Response Bubble Body
        Surface(
            color = SurfaceVariantLight,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Render main response text
                val parsedText = message.text
                if (parsedText.contains("• Declarative UI Architecture") || parsedText.contains("Declarative UI Architecture")) {
                    // Extract intro text
                    val introText = parsedText.substringBefore("• Declarative UI Architecture")
                        .substringBefore("Declarative UI Architecture").trim()
                    if (introText.isNotEmpty()) {
                        Text(
                            text = introText.toBoldAnnotatedString(),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Feature Card 1
                    FeatureCard(
                        icon = Icons.Default.Code,
                        iconTint = PrimaryBlue,
                        title = "Declarative UI Architecture",
                        description = "Code natively mirrors mutable state transitions with zero repetitive imperative boilerplate."
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Feature Card 2
                    FeatureCard(
                        icon = Icons.Default.Palette,
                        iconTint = Color(0xFF8E24AA),
                        title = "Material You Dynamic Theming",
                        description = "Instant algorithmic extraction for dynamic color schemes, tonal elevation, and adaptive typography tokens."
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Feature Card 3
                    FeatureCard(
                        icon = Icons.Default.Bolt,
                        iconTint = Color(0xFF1976D2),
                        title = "Built-in Compiler Performance",
                        description = "Smart incremental recomposition with a flat layout hierarchy that avoids traditional deep view nesting."
                    )
                } else {
                    Text(
                        text = message.text.toBoldAnnotatedString(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color(0xFF5C6068),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    Toast
                                        .makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        )
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            tint = Color(0xFF5C6068),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {}
                        )
                        Icon(
                            imageVector = Icons.Outlined.ThumbDown,
                            contentDescription = "Dislike",
                            tint = Color(0xFF5C6068),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {}
                        )
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF5C6068),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {}
                        )
                    }

                    // Modify button pill
                    Surface(
                        color = Color(0xFFE2E7F4),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {}
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Modify",
                                tint = Color(0xFF424752),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Modify",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF424752)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
) {
    Surface(
        color = CardSurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF43474E)
                )
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Thinking Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = "Gemini",
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Gemini",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• Thinking",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = PrimaryBlue
            )
        }

        Surface(
            color = SurfaceVariantLight,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    color = CardSurfaceWhite,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Drafting Composable architecture...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Writing Jetpack Compose Surface dock & TextField animation",
                                fontSize = 12.sp,
                                color = Color(0xFF43474E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = PrimaryBlue,
                    trackColor = PrimaryBlue.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun SuggestionsRow(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "🔗 Add enter transitions",
        "🎨 Apply dynamic palette",
        "⚡ Optimize recomposition"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            Surface(
                color = Color(0xFFEFF3FA),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFDCE2F9)),
                modifier = Modifier.clickable { onSuggestionClick(suggestion) }
            ) {
                Text(
                    text = suggestion,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF191C20),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomInputBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = CardSurfaceWhite,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E7F4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add attachment",
                    tint = Color(0xFF43474E),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (prompt.isEmpty()) {
                    Text(
                        text = if (promptError != null) "Type a message... (Cannot be empty)" else "Type a message...",
                        color = if (promptError != null) MaterialTheme.colorScheme.error else Color(0xFF8E919A),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    enabled = enabled,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {},
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = Color(0xFF43474E),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
                shape = CircleShape,
                color = if (enabled && prompt.isNotBlank()) PrimaryBlue else PrimaryBlue.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(enabled = enabled) { onSend() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessageEntity(
                        id = 1,
                        text = "Hey Gemini! Can you summarize the key advantages of Jetpack Compose with Material 3 for mobile UI development?",
                        isFromUser = true,
                        formattedTime = "10:24 AM"
                    ),
                    ChatMessageEntity(
                        id = 2,
                        text = "Jetpack Compose combined with Material 3 (Material You) delivers a modern, reactive toolkit engineered for modern Android architecture:\n\n• Declarative UI Architecture: Code natively mirrors mutable state transitions with zero repetitive imperative boilerplate.\n\n• Material You Dynamic Theming: Instant algorithmic extraction for dynamic color schemes, tonal elevation, and adaptive typography tokens.\n\n• Built-in Compiler Performance: Smart incremental recomposition with a flat layout hierarchy that avoids traditional deep view nesting.",
                        isFromUser = false,
                        formattedTime = "10:24 AM"
                    )
                )
            ),
            onPromptChange = {},
            onSend = {},
        )
    }
}
