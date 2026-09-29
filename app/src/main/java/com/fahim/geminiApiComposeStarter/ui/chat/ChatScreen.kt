package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.AttachmentUtils
import com.fahim.geminiApiComposeStarter.data.ChatAttachment
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatSession
import com.fahim.geminiApiComposeStarter.ui.text.MarkdownMessageView
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onNewChat = viewModel::onNewChat,
        onSwitchSession = viewModel::onSwitchSession,
        onDeleteSession = viewModel::onDeleteSession,
        onOpenModelDialog = { viewModel.onShowModelDialog(true) },
        onSelectModel = viewModel::onSelectModel,
        onDismissModelDialog = { viewModel.onShowModelDialog(false) },
        onShowAttachmentPicker = viewModel::onShowAttachmentPicker,
        onAddAttachments = viewModel::onAddAttachments,
        onRemoveAttachment = viewModel::onRemoveAttachment,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onNewChat: () -> Unit,
    onSwitchSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenModelDialog: () -> Unit,
    onSelectModel: (String) -> Unit,
    onDismissModelDialog: () -> Unit,
    onShowAttachmentPicker: (Boolean) -> Unit,
    onAddAttachments: (List<ChatAttachment>) -> Unit,
    onRemoveAttachment: (ChatAttachment) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && tempCameraUri != null) {
            val uri = tempCameraUri!!
            val name = AttachmentUtils.getFileName(context, uri)
            onAddAttachments(listOf(ChatAttachment(uriString = uri.toString(), name = name, isImage = true)))
        }
    }

    val multiplePhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val attachments = uris.map { uri ->
                val name = AttachmentUtils.getFileName(context, uri)
                ChatAttachment(uriString = uri.toString(), name = name, isImage = true)
            }
            onAddAttachments(attachments)
        }
    }

    val filesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val attachments = uris.map { uri ->
                val isImage = AttachmentUtils.isImageUri(context, uri)
                val name = AttachmentUtils.getFileName(context, uri)
                ChatAttachment(uriString = uri.toString(), name = name, isImage = isImage)
            }
            onAddAttachments(attachments)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        val total = state.messages.size + if (state.isLoading) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    if (state.showModelDialog) {
        ModelSelectionDialog(
            currentModel = state.selectedModel,
            onDismiss = onDismissModelDialog,
            onModelSelected = onSelectModel,
        )
    }

    if (state.showAttachmentPicker) {
        AttachmentBottomSheet(
            onDismiss = { onShowAttachmentPicker(false) },
            onCameraClick = {
                onShowAttachmentPicker(false)
                val uri = AttachmentUtils.createTempCameraUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            },
            onGalleryClick = {
                onShowAttachmentPicker(false)
                multiplePhotosLauncher.launch("image/*")
            },
            onFilesClick = {
                onShowAttachmentPicker(false)
                filesLauncher.launch("*/*")
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(305.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_gemini_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini Composer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                            ),
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onNewChat()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Chat", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Recent Conversations",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.sessions) { session ->
                            val isSelected = session.id == state.currentSessionId
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.SansSerif,
                                        ),
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onSwitchSession(session.id)
                                    scope.launch { drawerState.close() }
                                },
                                badge = {
                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Colorful Animated Google Gemini Button
                    GeminiExploreButton(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Gemini",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                scope.launch { drawerState.open() }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Chat History",
                            )
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onOpenModelDialog()
                            },
                            label = {
                                Text(
                                    text = state.selectedModel,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Model",
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onNewChat()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_gemini_logo),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "How can I help you today?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.messages) { message ->
                            ChatMessageBubble(message = message)
                        }
                        if (state.isLoading) {
                            item {
                                LoadingAssistantBubble()
                            }
                        }
                    }
                }

                // Staged / Pending Attachments Carousel above input bar
                if (state.pendingAttachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.pendingAttachments.forEach { att ->
                            AttachmentChip(
                                attachment = att,
                                onRemove = { onRemoveAttachment(att) },
                            )
                        }
                    }
                }

                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    hasAttachments = state.pendingAttachments.isNotEmpty(),
                    onPromptChange = onPromptChange,
                    onAttachClick = { onShowAttachmentPicker(true) },
                    onSend = onSend,
                )
            }
        }
    }
}

/** Launches the Google Gemini app, or redirects to Google Play Store if not installed */
private fun openGeminiApp(context: android.content.Context) {
    val packageName = "com.google.android.apps.bard"
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}

/** Animated colorful border button with thicker 3.5dp border and seamless repeated animation */
@Composable
private fun GeminiExploreButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_border_anim")
    val gradientSpan = 800f
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = gradientSpan,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "border_offset",
    )

    // Google Gemini 4-color emblem palette seamlessly repeated
    val geminiColors = listOf(
        Color(0xFFEA4335), // Red
        Color(0xFF4285F4), // Blue
        Color(0xFF34A853), // Green
        Color(0xFFFBBC04), // Yellow / Amber
        Color(0xFFEA4335), // Loop Red
    )

    val animatedBorderBrush = Brush.linearGradient(
        colors = geminiColors,
        start = Offset(offset, 0f),
        end = Offset(offset + gradientSpan, gradientSpan * 0.35f),
        tileMode = TileMode.Repeated,
    )

    Surface(
        onClick = { openGeminiApp(context) },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(width = 3.5.dp, brush = animatedBorderBrush),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gemini_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Open Gemini",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.2.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: ChatAttachment,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            if (attachment.isImage) {
                ThumbnailImage(uriString = attachment.uriString, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)))
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_file),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp),
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ThumbnailImage(uriString: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, uriString) {
        value = withContext(Dispatchers.IO) {
            AttachmentUtils.loadScaledBitmap(context, Uri.parse(uriString), maxDimension = 120)
        }
    }
    bitmapState.value?.let { bm ->
        Image(
            bitmap = bm.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } ?: Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (message.attachments.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            message.attachments.forEach { att ->
                                if (att.isImage) {
                                    ThumbnailImage(
                                        uriString = att.uriString,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_file),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = att.name,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.SansSerif),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (message.text.isNotBlank() && message.text != "Analyze this attachment") {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gemini_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 4.dp, end = 6.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Surface(
                shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.92f),
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    MarkdownMessageView(text = message.text)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFilesClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Add Attachments",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                ),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCameraClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Take Photo", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif))
                    Text(text = "Capture an image with your camera", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGalleryClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gallery),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Upload Photos", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif))
                    Text(text = "Choose one or multiple photos from gallery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFilesClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_file),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Upload Files", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif))
                    Text(text = "Select documents, text, code, or other files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LoadingAssistantBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_gemini_logo),
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .padding(end = 6.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionDialog(
    currentModel: String,
    onDismiss: () -> Unit,
    onModelSelected: (String) -> Unit,
) {
    var selectedModelId by remember { mutableStateOf(currentModel) }
    var isCustom by remember {
        mutableStateOf(PRESET_MODELS.none { it.id == currentModel })
    }
    var customModelText by remember {
        mutableStateOf(if (isCustom) currentModel else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_gemini_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = "Select Model", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                PRESET_MODELS.forEach { model ->
                    val isChecked = !isCustom && selectedModelId == model.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isCustom = false
                                selectedModelId = model.id
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isChecked,
                            onClick = {
                                isCustom = false
                                selectedModelId = model.id
                            },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.SansSerif,
                                ),
                            )
                            Text(
                                text = model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Custom Model Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustom = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isCustom,
                        onClick = { isCustom = true },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Custom Model",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.SansSerif,
                            ),
                        )
                        if (isCustom) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customModelText,
                                onValueChange = { customModelText = it },
                                placeholder = { Text("e.g. gemini-3.8-flash") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalModel = if (isCustom) customModelText.trim() else selectedModelId
                    if (finalModel.isNotBlank()) {
                        onModelSelected(finalModel)
                    }
                },
            ) {
                Text("Apply", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = FontFamily.SansSerif)
            }
        },
    )
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    hasAttachments: Boolean,
    onPromptChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val updatedPrompt = if (prompt.isBlank()) spokenText else "$prompt $spokenText"
                onPromptChange(updatedPrompt)
            }
        }
    }

    val canSend = (prompt.isNotBlank() || hasAttachments) && enabled

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Attachment Plus Button
        IconButton(
            onClick = onAttachClick,
            enabled = enabled,
            modifier = Modifier.padding(end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Attach files or photos",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            placeholder = { Text("Message Gemini...", fontFamily = FontFamily.SansSerif) },
            minLines = 1,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            enabled = enabled,
            isError = promptError != null,
            trailingIcon = {
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Voice input is not supported on this device",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    enabled = enabled,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "Voice input",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        Surface(
            shape = CircleShape,
            color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(48.dp)
                .clickable(enabled = canSend, onClick = onSend),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
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
                sessions = listOf(ChatSession(title = "Kotlin Comparison")),
                messages = listOf(
                    ChatMessage(text = "Can you compare Kotlin and Java in a table?", isUser = true),
                    ChatMessage(
                        text = "| Feature | Kotlin | Java |\n|---|---|---|\n| Null Safety | Built-in | Annotations |\n| Syntax | Concise | Verbose |",
                        isUser = false,
                    ),
                ),
            ),
            onPromptChange = {},
            onSend = {},
            onNewChat = {},
            onSwitchSession = {},
            onDeleteSession = {},
            onOpenModelDialog = {},
            onSelectModel = {},
            onDismissModelDialog = {},
            onShowAttachmentPicker = {},
            onAddAttachments = {},
            onRemoveAttachment = {},
        )
    }
}
