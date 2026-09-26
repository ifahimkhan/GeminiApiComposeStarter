package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.ui.components.*
import com.fahim.geminiApiComposeStarter.ui.theme.*
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(state: ChatUiState, actions: ChatActions) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { actions.navigate(Destination.SETTINGS) }, modifier = Modifier.size(49.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF69889E), Color(0xFF253554))))) {
                        Icon(Icons.Outlined.Person, "Profile", tint = Color(0xFFF2E7DF), modifier = Modifier.size(30.dp))
                    }
                    RoundControl(Icons.Outlined.NotificationsNone, "Notifications", { actions.sheet(Sheet.NOTICES) })
                }
                Text("How can I assist\nyou today?", color = Color.White, fontSize = 36.sp, lineHeight = 43.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp, modifier = Modifier.padding(start = 22.dp, top = 25.dp, bottom = 25.dp, end = 16.dp))
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listOf(Category.RESEARCH, Category.HEALTH), key = { it.name }) { category ->
                        GlassPanel(Modifier.width(255.dp).heightIn(min = 205.dp), radius = 12.dp) {
                            Column(Modifier.padding(14.dp)) {
                                CategoryBadge(category)
                                Spacer(Modifier.height(13.dp))
                                Text(category.title, color = Color.White, fontSize = 19.sp, maxLines = 1, letterSpacing = (-.6).sp)
                                Spacer(Modifier.height(6.dp))
                                Text(category.subtitle, color = MutedText, fontSize = 12.sp, lineHeight = 15.sp, minLines = 3, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(12.dp))
                                Box(Modifier.fillMaxWidth().height(39.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Color(0xFF67A7F1), Color(0xFF096EFF))))
                                    .border(.7.dp, Color(0xFF80B8FC), RoundedCornerShape(10.dp)).judyClickable { actions.newChat(category, true) }, contentAlignment = Alignment.Center) { Text("Generate", fontSize = 14.sp, color = Color.White) }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 22.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("History", color = Color.White, fontSize = 21.sp)
                    TextButton(onClick = { actions.navigate(Destination.HISTORY) }) { Text("See all", color = Color(0xFFB9BECA), fontSize = 13.sp, textDecoration = TextDecoration.Underline) }
                }
            }
            if (state.conversations.isEmpty()) item {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
                    Text("A fresh start for your next idea", fontSize = 16.sp, color = Color.White)
                    Text("Your conversations will appear here.", fontSize = 13.sp, color = MutedText, modifier = Modifier.padding(top = 6.dp))
                }
            } else items(state.conversations.take(3), key = { it.id }) { conversation ->
                HistoryRow(conversation, { actions.openConversation(conversation) }, Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
            }
        }
        NavigationDock(state.destination, actions.navigate, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun NavigationDock(selected: Destination, navigate: (Destination) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(100.dp)) {
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(76.dp).clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(DockColor.copy(alpha = .97f)).border(.6.dp, Color(0xFF424B61), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem("Home", Icons.Outlined.Home, selected == Destination.HOME, { navigate(Destination.HOME) }, Modifier.weight(1f))
            NavItem("Search", Icons.Outlined.TravelExplore, selected == Destination.SEARCH, { navigate(Destination.SEARCH) }, Modifier.weight(1f))
            Spacer(Modifier.width(88.dp))
            NavItem("History", Icons.Outlined.Storage, selected == Destination.HISTORY, { navigate(Destination.HISTORY) }, Modifier.weight(1f))
            NavItem("Setting", Icons.Outlined.Settings, selected == Destination.SETTINGS, { navigate(Destination.SETTINGS) }, Modifier.weight(1f))
        }
        HexMicButton({ navigate(Destination.VOICE) }, Modifier.align(Alignment.TopCenter))
    }
}
@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(if (selected) Color.White else Color(0xFF858CBA), if (LocalReducedMotion.current) snap() else tween(180), label = "navigation selection")
    Column(modifier.clip(RoundedCornerShape(16.dp)).judyClickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        Text(label, fontSize = 10.sp, color = if (selected) Color.White else MutedText, modifier = Modifier.padding(top = 4.dp))
    }
}
@Composable
internal fun HistoryRow(conversation: Conversation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassPanel(modifier.fillMaxWidth().clip(RoundedCornerShape(40.dp)).judyClickable(onClick = onClick), radius = 40.dp) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(conversation.category, Modifier.size(40.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(conversation.title, fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(conversation.preview, fontSize = 12.sp, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}
@Composable
internal fun HistoryList(conversations: List<Conversation>, open: (Conversation) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 500.dp)) {
        items(conversations, key = { it.id }) { HistoryRow(it, { open(it) }) }
    }
}

@Composable
internal fun ConversationScreen(state: ChatUiState, actions: ChatActions) {
    val showPending = state.isLoading && (state.conversationId == null || state.messages.any { it.turnId == state.pendingTurnId })
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val atBottom by remember { derivedStateOf { !list.canScrollForward } }
    LaunchedEffect(state.conversationId, state.messages.size, state.pendingTurnId) {
        if (state.messages.isNotEmpty() && (atBottom || state.isLoading)) {
            if (state.preferences.reducedMotion) list.scrollToItem(state.messages.lastIndex) else list.animateScrollToItem(state.messages.lastIndex)
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            RoundControl(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Back", actions.back)
            ModelPill(state) { actions.sheet(Sheet.MODEL) }
            RoundControl(NewChatIcon, "New chat", { actions.newChat(state.preferences.category, false) })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize().testTag("messages"), state = list, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Bottom)) {
                items(state.messages, key = { it.id }) { message ->
                    Box(if (state.preferences.reducedMotion) Modifier else Modifier.animateItem(fadeInSpec = tween(240), placementSpec = spring(stiffness = 450f), fadeOutSpec = tween(120))) { MessageBubble(message, state, actions) }
                }

            }
            if (!atBottom && state.messages.isNotEmpty()) FilledTonalIconButton(onClick = { scope.launch { list.animateScrollToItem(state.messages.lastIndex) } }, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                Icon(Icons.Outlined.ArrowDownward, "Latest message")
            }
        }
        AnimatedVisibility(showPending, enter = if (state.preferences.reducedMotion) EnterTransition.None else fadeIn(tween(180)) + expandVertically(), exit = if (state.preferences.reducedMotion) ExitTransition.None else fadeOut(tween(120)) + shrinkVertically()) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.preferences.reducedMotion) CircularProgressIndicator(progress = { .75f }, modifier = Modifier.size(20.dp).testTag("loading"), color = ElectricBlue, strokeWidth = 2.dp)
                else CircularProgressIndicator(Modifier.size(20.dp).testTag("loading"), color = ElectricBlue, strokeWidth = 2.dp)
                Text("Thinking…", color = MutedText, fontSize = 12.sp)
            }
        }
        state.promptError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) }
        ChatComposer(state.prompt, state.isInitializing || state.isLoading, actions.prompt, actions.send, { actions.sheet(Sheet.ADD) })
    }
}
@Composable
private fun MessageBubble(message: ChatMessage, state: ChatUiState, actions: ChatActions) {
    val lastUser = state.messages.lastOrNull { it.role == Role.USER }
    if (message.role == Role.USER) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Surface(modifier = Modifier.widthIn(max = 285.dp), shape = RoundedCornerShape(10.dp), color = Color(0xCC262A2D), border = BorderStroke(.7.dp, Color(0xFF6C7279))) {
                Text(message.text, color = Color(0xFFD4D5D9), fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.padding(12.dp))
            }
            if (message.delivery == Delivery.FAILED || message.delivery == Delivery.INTERRUPTED) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (message.delivery == Delivery.INTERRUPTED) "Interrupted" else "Couldn't send", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    if (lastUser?.id == message.id) TextButton(onClick = { actions.retry(message) }, enabled = !state.isLoading) { Text("Retry", fontSize = 12.sp) }
                }
            }
        }
    } else {
        Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(message.text.toBoldAnnotatedString(), color = Color(0xFFD0D3D9), fontSize = 14.sp, lineHeight = 19.sp)
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    AnswerAction(Icons.Outlined.ThumbUp, "Like answer", { actions.feedback(message, 1) }, selected = message.feedback == 1)
                    AnswerAction(Icons.Outlined.ThumbDown, "Dislike answer", { actions.feedback(message, -1) }, selected = message.feedback == -1)
                    Spacer(Modifier.weight(1f))
                    AnswerAction(Icons.Outlined.Share, "Share answer", { actions.share(message.text) })
                    AnswerAction(Icons.Outlined.Refresh, if (lastUser?.turnId == message.turnId) "Regenerate answer" else "Only the latest answer can be regenerated", actions.regenerate, enabled = !state.isLoading && lastUser?.turnId == message.turnId)
                    AnswerAction(Icons.Outlined.ContentCopy, "Copy answer", { actions.copy(message.text) })
                }
            }
        }
    }
}
@Composable
private fun AnswerAction(icon: ImageVector, label: String, action: () -> Unit, selected: Boolean = false, enabled: Boolean = true) {
    IconButton(onClick = action, enabled = enabled, modifier = Modifier.size(48.dp)) { Icon(icon, label, modifier = Modifier.size(18.dp), tint = if (!enabled) Color(0xFF41444A) else if (selected) ElectricBlue else Color(0xFF8D929B)) }
}
@Composable
private fun ChatComposer(prompt: String, busy: Boolean, onChange: (String) -> Unit, send: () -> Unit, add: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).background(DockColor)
        .border(.7.dp, Brush.verticalGradient(listOf(Color(0xFF737D93), Color.Transparent)), RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 25.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF2B344C)).border(.7.dp, Color(0xFF4A5267), RoundedCornerShape(28.dp)), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = add) { Icon(Icons.Outlined.Add, "More input options", tint = Color(0xFFD5D9E4)) }
            BasicTextField(value = prompt, onValueChange = onChange, modifier = Modifier.weight(1f).padding(vertical = 13.dp).testTag("prompt"), textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(ElectricBlue), maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if (!busy) send() }),
                decorationBox = { inner -> Box { if (prompt.isEmpty()) Text("Ask anything", color = Color(0xFF939AAA), fontSize = 14.sp); inner() } })
            IconButton(onClick = send, enabled = !busy, modifier = Modifier.padding(3.dp).size(46.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF86BAFF), ElectricBlue, Color(0xFF0063DF)))).border(.6.dp, Color(0xFF8DB9EE), CircleShape).testTag("send")) {
                Icon(Icons.Filled.NearMe, "Send", tint = if (busy) Color(0xFFACC8EB) else Color.White, modifier = Modifier.size(23.dp))
            }
        }
    }
}

@Composable
internal fun VoiceScreen(state: ChatUiState, actions: ChatActions) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundControl(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Back", actions.back)
            Text("Talking with Judy", color = Color.White, fontSize = 16.sp)
            RoundControl(CrownIcon, "About Judy", { actions.sheet(Sheet.ASSISTANT) })
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val compactHeight = maxHeight < 400.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(if (compactHeight) 8.dp else 24.dp))
                Text(when (state.voiceStatus) {
                    VoiceStatus.READY -> "Ready to listen"
                    VoiceStatus.WAITING -> "Waiting for speech recognition…"
                    VoiceStatus.TRANSCRIPT -> "Ready when you are"
                    VoiceStatus.CANCELLED -> "Tap the microphone to try again"
                    VoiceStatus.UNAVAILABLE -> "Let's type instead"
                }, color = MutedText, fontSize = 14.sp)
                Spacer(Modifier.height(if (compactHeight) 4.dp else 12.dp))
                VoiceOrb(state.preferences.reducedMotion, Modifier.width(if (compactHeight) 200.dp else 320.dp))
                AnimatedContent(targetState = state.transcript, transitionSpec = {
                    if (state.preferences.reducedMotion) EnterTransition.None togetherWith ExitTransition.None
                    else (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 5 }) togetherWith fadeOut(tween(120))
                }, label = "voice transcript") { transcript ->
                if (transcript.isNotBlank()) Text(transcript, fontSize = 24.sp, lineHeight = 31.sp, textAlign = TextAlign.Center, color = Color(0xFFE5E7EE), modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                else Text("A thought, a question,\na new possibility.", fontSize = 23.sp, lineHeight = 30.sp, textAlign = TextAlign.Center, color = Color(0xFFBDC2CE), modifier = Modifier.padding(top = 12.dp))
                }
                Text(if (state.transcript.isBlank()) "Tap the microphone to get started" else "Open chat to review and send", fontSize = 12.sp, color = MutedText, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp)).background(DockColor)
            .border(1.dp, Brush.verticalGradient(listOf(Color(0xFF7D8496), Color.Transparent)), RoundedCornerShape(topStart = 52.dp, topEnd = 52.dp)), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 44.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RoundControl(Icons.AutoMirrored.Outlined.Chat, "Open chat", actions.useTranscript)
                HexMicButton(actions.recognize, Modifier.size(110.dp), enabled = state.voiceStatus != VoiceStatus.WAITING, diameter = 96.dp)
                RoundControl(Icons.Outlined.Close, "Close voice", actions.back)
            }
        }
    }
}

@Composable
internal fun HistoryScreen(state: ChatUiState, actions: ChatActions) {
    val search = state.destination == Destination.SEARCH
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundControl(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Back", actions.back)
            Text(if (search) "Find a conversation" else "Your conversations", color = Color.White, fontSize = 23.sp, modifier = Modifier.padding(start = 14.dp))
        }
        if (search) OutlinedTextField(state.searchQuery, actions.search, placeholder = { Text("Search your ideas") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp))
        val conversations = if (search) state.searchResults else state.conversations
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (conversations.isEmpty()) item { Column(Modifier.padding(vertical = 56.dp)) {
                Icon(Icons.Outlined.Forum, null, tint = ElectricBlue, modifier = Modifier.size(36.dp))
                Text(if (search && state.searchQuery.isNotBlank()) "No conversations found" else "Every idea starts somewhere", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(top = 16.dp))
                Text(if (search && state.searchQuery.isNotBlank()) "Try another word or phrase." else "Ask your first question and come back to it here.", color = MutedText, modifier = Modifier.padding(top = 10.dp))
                TextButton(onClick = { actions.newChat(state.preferences.category, false) }) { Text("Start a conversation") }
            } }
            items(conversations, key = { it.id }) { HistoryRow(it, { actions.openConversation(it) }) }
        }
        NavigationDock(state.destination, actions.navigate)
    }
}
@Composable
internal fun SettingsScreen(state: ChatUiState, actions: ChatActions) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundControl(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Back", actions.back)
            Text("Make it yours", fontSize = 28.sp, color = Color.White, modifier = Modifier.padding(start = 16.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Text("A little more personal.", color = MutedText, modifier = Modifier.padding(bottom = 30.dp))
            OutlinedTextField(state.preferences.displayName, { actions.preferences(state.preferences.copy(displayName = it)) }, label = { Text("Your name") }, supportingText = { Text("Saved only on this device") }, shape = RoundedCornerShape(16.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            GlassPanel(Modifier.fillMaxWidth().judyClickable { actions.sheet(Sheet.MODEL) }, radius = 20.dp) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFFAEC9FF))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Your model", color = MutedText, fontSize = 12.sp)
                        Text(GeminiModels.selected(state.preferences.modelId).title, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Choose model", tint = Color(0xFFAEC9FF))
                }
            }
            Text("Your preferred assistant", fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
            Category.entries.forEach { category ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { actions.preferences(state.preferences.copy(category = category)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category)
                    Text(category.title, modifier = Modifier.weight(1f).padding(start = 12.dp), color = Color.White, fontSize = 15.sp)
                    RadioButton(selected = state.preferences.category == category, onClick = { actions.preferences(state.preferences.copy(category = category)) })
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Color(0xFF283148))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Reduce motion", color = Color.White, fontSize = 18.sp); Text("Pause ambient motion and screen effects", color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                Switch(state.preferences.reducedMotion, { actions.preferences(state.preferences.copy(reducedMotion = it)) })
            }
            Text("Judy AI", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(top = 42.dp))
            Text("A little clarity. A new perspective.", color = MutedText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Text("Conversations stay on this device. Questions are sent to Gemini when you tap Send. Speech recognition is handled by your device's speech provider.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 16.dp, bottom = 30.dp))
        }
        NavigationDock(state.destination, actions.navigate)
    }
}
