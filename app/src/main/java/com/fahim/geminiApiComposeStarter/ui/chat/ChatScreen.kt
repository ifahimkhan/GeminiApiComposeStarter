package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.ui.components.*
import com.fahim.geminiApiComposeStarter.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

/** Pure callbacks keep screens independent of the ViewModel, Android launchers and repositories. */
data class ChatActions(
    val navigate: (Destination) -> Unit = {}, val back: () -> Unit = {},
    val newChat: (Category, Boolean) -> Unit = { _, _ -> }, val openConversation: (Conversation) -> Unit = {},
    val prompt: (String) -> Unit = {}, val send: () -> Unit = {}, val retry: (ChatMessage) -> Unit = {},
    val regenerate: () -> Unit = {}, val feedback: (ChatMessage, Int) -> Unit = { _, _ -> },
    val copy: (String) -> Unit = {}, val share: (String) -> Unit = {},
    val search: (String) -> Unit = {}, val preferences: (UserPreferences) -> Unit = {},
    val model: (String) -> Unit = {},
    val sheet: (Sheet) -> Unit = {}, val category: (Category) -> Unit = {},
    val recognize: () -> Unit = {}, val useTranscript: () -> Unit = {},
)

@Composable
fun ChatRoute(viewModel: ChatViewModel, windowSize: WindowSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onVoiceResult(if (result.resultCode == Activity.RESULT_OK) result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() else null)
    }
    val actions = ChatActions(
        navigate = viewModel::navigate, back = viewModel::back, newChat = viewModel::newChat,
        openConversation = viewModel::openConversation, prompt = viewModel::onPromptChange,
        send = viewModel::onSend, retry = viewModel::retry, regenerate = viewModel::regenerate,
        feedback = viewModel::feedback, copy = { clipboard.setText(AnnotatedString(it)); scope.launch { snackbar.showSnackbar("Copied to clipboard") } },
        share = { text -> try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share answer")) } catch (_: ActivityNotFoundException) { scope.launch { snackbar.showSnackbar("No sharing app is available.") } } },
        search = viewModel::onSearchChange, preferences = viewModel::savePreferences,
        model = viewModel::selectModel, sheet = viewModel::showSheet, category = viewModel::selectCategory, useTranscript = viewModel::useTranscript,
        recognize = {
            try {
                viewModel.onVoiceStarted()
                launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "What would you like to ask Judy?")
                })
            } catch (_: ActivityNotFoundException) { viewModel.onVoiceUnavailable() }
            catch (_: SecurityException) { viewModel.onVoiceUnavailable() }
        },
    )
    BackHandler(state.destination != Destination.HOME || state.sheet != Sheet.NONE) {
        if (state.sheet != Sheet.NONE) viewModel.showSheet(Sheet.NONE) else viewModel.back()
    }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.dismissError() } }
    Box(Modifier.fillMaxSize()) {
        ChatScreen(state, actions, windowSize.widthSizeClass == WindowWidthSizeClass.Expanded)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(bottom = 110.dp, start = 16.dp, end = 16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(state: ChatUiState, actions: ChatActions, expanded: Boolean = false) {
    CompositionLocalProvider(LocalReducedMotion provides state.preferences.reducedMotion) {
        Box(Modifier.fillMaxSize()) {
            ReferenceBackground()
            Row(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                if (expanded) {
                    Column(Modifier.width(280.dp).fillMaxHeight().background(Color(0xAA050D1A)).padding(22.dp)) {
                        Text("Judy", fontSize = 30.sp, color = Color.White)
                        Text("A space for your ideas", color = MutedText, fontSize = 13.sp)
                        Spacer(Modifier.height(28.dp))
                        OutlinedButton(onClick = { actions.newChat(state.preferences.category, false) }) { Icon(Icons.Outlined.Add, null); Text("New conversation") }
                        Spacer(Modifier.height(20.dp))
                        HistoryList(state.conversations, actions.openConversation)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { actions.navigate(Destination.SETTINGS) }) { Text("Settings") }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = if (state.destination == Destination.CHAT) 780.dp else 560.dp).fillMaxSize()) {
                        AnimatedContent(targetState = state.destination, modifier = Modifier.fillMaxSize(), transitionSpec = {
                            if (state.preferences.reducedMotion) EnterTransition.None togetherWith ExitTransition.None
                            else (fadeIn(tween(220, 60)) + slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 35 }) togetherWith fadeOut(tween(130))
                        }, label = "screen transition") { destination ->
                            val screenState = state.copy(destination = destination)
                            when (destination) {
                                Destination.HOME -> HomeScreen(screenState, actions)
                                Destination.CHAT -> ConversationScreen(screenState, actions)
                                Destination.VOICE -> VoiceScreen(screenState, actions)
                                Destination.HISTORY, Destination.SEARCH -> HistoryScreen(screenState, actions)
                                Destination.SETTINGS -> SettingsScreen(screenState, actions)
                            }
                        }
                    }
                }
            }
            if (state.sheet != Sheet.NONE) ModalBottomSheet(onDismissRequest = { actions.sheet(Sheet.NONE) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color(0xFF0D1525), scrimColor = Color.Black.copy(alpha = .7f)) {
                if (state.sheet == Sheet.MODEL) ModelPicker(state, actions) else Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    when (state.sheet) {
                        Sheet.HOST -> {
                            Text("Choose your assistant", fontSize = 24.sp)
                            Category.entries.forEach { category -> ListItem(headlineContent = { Text(category.title) }, leadingContent = { CategoryBadge(category) }, modifier = Modifier.clickable { actions.category(category) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
                        }
                        Sheet.ADD -> {
                            Text("Make room for an idea", fontSize = 24.sp)
                            TextButton(onClick = { actions.navigate(Destination.VOICE) }) { Icon(Icons.Outlined.Mic, null); Text("  Use your voice") }
                            TextButton(onClick = { actions.prompt(state.category.suggestion.ifBlank { "Help me explore an idea from a new perspective." }); actions.sheet(Sheet.NONE) }) { Icon(Icons.Outlined.AutoAwesome, null); Text("  Try a suggested prompt") }
                        }
                        Sheet.NOTICES -> { Text("You're all caught up", fontSize = 24.sp); Spacer(Modifier.height(12.dp)); Text("Your ideas and conversations are right here whenever you need them.", color = MutedText) }
                        Sheet.ASSISTANT -> { Text("Meet Judy", fontSize = 26.sp); Spacer(Modifier.height(12.dp)); Text("Your AI host for questions, research and everyday curiosity. Powered by Gemini. Answers can make mistakes — check important information.", color = MutedText) }
                        Sheet.MODEL, Sheet.NONE -> Unit
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
