package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ── Live Voice State ──────────────────────────────────────────────────────────

enum class LiveVoicePhase {
    IDLE,        // Session just opened, waiting for user to speak
    LISTENING,   // Mic open, user is speaking
    THINKING,    // Gemini processing the user turn
    SPEAKING,    // TTS is playing Gemini's reply
    ENDED,       // Session ended, converting to chat
}

data class LiveTurn(
    val userText: String,
    val modelText: String,
)

// ── Animated orb ─────────────────────────────────────────────────────────────

@Composable
fun LiveVoiceOrb(
    phase: LiveVoicePhase,
    amplitude: Float,   // 0f..1f
    modifier: Modifier = Modifier,
) {
    var smoothedAmplitude by remember { mutableFloatStateOf(amplitude) }
    LaunchedEffect(amplitude) {
        smoothedAmplitude += (amplitude - smoothedAmplitude) * 0.25f
    }
    val infiniteTransition = rememberInfiniteTransition(label = "voice-orb")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "voice-orb-time",
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "voice-orb-breathe",
    )
    val phaseBoost = when (phase) {
        LiveVoicePhase.LISTENING -> 0.55f
        LiveVoicePhase.THINKING -> 0.18f
        LiveVoicePhase.SPEAKING -> 0.35f
        LiveVoicePhase.IDLE -> 0.08f
        LiveVoicePhase.ENDED -> 0f
    }
    val finalScale = breathe * (1f + smoothedAmplitude * phaseBoost)

    Canvas(
        modifier = modifier
            .graphicsLayer {
                scaleX = finalScale
                scaleY = finalScale
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .blur(radius = 4.dp)
    ) {
        val radiusPx = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF4A63F5), Color(0xFF8AA0FF), Color(0xFFEFF3FF)),
                center = center,
                radius = radiusPx,
            ),
            radius = radiusPx,
            center = center,
        )

        val blobs = listOf(
            Triple(Color(0xFF6E7BFF), 0.0f, 0.9f),
            Triple(Color.White, 0.33f, 0.6f),
            Triple(Color(0xFF3F51E0), 0.66f, 0.8f),
        )
        blobs.forEach { (color, phaseOffset, blobRadiusFactor) ->
            val angle = (time + phaseOffset) * 2f * Math.PI.toFloat()
            val orbitRadius = radiusPx * (0.35f + smoothedAmplitude * 0.25f)
            val blobCenter = Offset(
                x = center.x + cos(angle) * orbitRadius,
                y = center.y + sin(angle) * orbitRadius,
            )
            val blobRadius = radiusPx * blobRadiusFactor * (0.55f + smoothedAmplitude * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    center = blobCenter,
                    radius = blobRadius,
                ),
                radius = blobRadius,
                center = blobCenter,
                blendMode = BlendMode.Plus,
            )
        }
    }
}

// ── Live Voice Overlay Screen ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveVoiceScreen(
    state: ChatUiState,
    onDismiss: () -> Unit,
    onSessionComplete: (List<LiveTurn>) -> Unit,
    onCallGemini: suspend (List<ChatMessage>) -> String,
    onNewChat: () -> Unit = {},
    onSelectChat: (String) -> Unit = {},
    onDeleteChat: (String) -> Unit = {},
    onSaveSettings: (String?, Boolean, String, Boolean) -> Unit = { _, _, _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var settingsOpen by remember { mutableStateOf(false) }

    // ── Local State ───────────────────────────────────────────────────────────
    var phase by remember { mutableStateOf(LiveVoicePhase.IDLE) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var currentTranscript by remember { mutableStateOf("") }
    var currentModelText by remember { mutableStateOf("") }
    var turns by remember { mutableStateOf(listOf<LiveTurn>()) }
    var statusLabel by remember { mutableStateOf("Tap to speak") }

    // ── SpeechRecognizer ─────────────────────────────────────────────────────
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var micPermissionRequested by remember { mutableStateOf(false) }
    var heardVoiceInTurn by remember { mutableStateOf(false) }
    var lastVoiceAtMs by remember { mutableLongStateOf(0L) }
    var forcedStopRequested by remember { mutableStateOf(false) }

    // ── Gemini conversation context ───────────────────────────────────────────
    val conversationHistory = remember { mutableListOf<ChatMessage>() }
    var nextId by remember { mutableLongStateOf(1000L) }

    // ── Amplitude animation for THINKING / SPEAKING ───────────────────────────
    LaunchedEffect(phase) {
        when (phase) {
            LiveVoicePhase.THINKING -> {
                statusLabel = "Thinking…"
                var t = 0f
                while (phase == LiveVoicePhase.THINKING) {
                    amplitude = (0.4f + kotlin.math.sin(t.toDouble()).toFloat() * 0.3f)
                        .coerceIn(0.1f, 0.9f)
                    t += 0.12f
                    delay(60)
                }
            }
            LiveVoicePhase.SPEAKING -> statusLabel = "Speaking…"
            LiveVoicePhase.LISTENING -> statusLabel = "Listening…"
            LiveVoicePhase.IDLE -> {
                statusLabel = "Tap to speak"
                amplitude = 0f
            }
            LiveVoicePhase.ENDED -> {}
        }
    }

    // ── TTS init ─────────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsReady = true
            }
        }
        tts = t
        onDispose {
            t.stop()
            t.shutdown()
        }
    }

    // ── SpeechRecognizer init ─────────────────────────────────────────────────
    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        onDispose {
            try { speechRecognizer?.destroy() } catch (_: Exception) {}
        }
    }

    // ── Gemini call ───────────────────────────────────────────────────────────
    fun callGemini(userText: String) {
        phase = LiveVoicePhase.THINKING
        currentModelText = ""

        val userMsg = ChatMessage(nextId++, ChatRole.USER, userText)
        conversationHistory.add(userMsg)

        scope.launch {
            try {
                // Use the repository through the state context by building a local instance
                // We reuse the existing repository from the ViewModel via the state's data layer
                // For direct access we call through ChatRepository pattern
                // We build message context and stream through existing GeminiRepository
                // injected by state — here we approximate by building the call manually

                // Build the full context for the conversation
                currentModelText = onCallGemini(conversationHistory.toList()).ifBlank {
                    "I heard you, but Gemini did not return a reply."
                }
                val modelMsg = ChatMessage(nextId++, ChatRole.GEMINI, currentModelText)
                conversationHistory.add(modelMsg)
                turns = turns + LiveTurn(userText, currentModelText)

                phase = LiveVoicePhase.SPEAKING
                if (ttsReady) {
                    tts?.speak(currentModelText, TextToSpeech.QUEUE_FLUSH, null, "live_${nextId}")
                    delay(500)
                    while (tts?.isSpeaking == true) delay(100)
                }
                phase = LiveVoicePhase.IDLE
            } catch (_: Exception) {
                // Direct repository not accessible here — use the ViewModel callback pattern:
                // The onSessionComplete callback will re-inject messages into the ViewModel
                // which already has the repo. So we send the full conversation at the end.
                // For the TTS, we use a placeholder here.
                currentModelText = "I’m having trouble reaching Gemini right now."
                val modelMsg = ChatMessage(nextId++, ChatRole.GEMINI, currentModelText)
                conversationHistory.add(modelMsg)
                turns = turns + LiveTurn(userText, currentModelText)

                phase = LiveVoicePhase.SPEAKING
                if (ttsReady) {
                    tts?.speak(currentModelText, TextToSpeech.QUEUE_FLUSH, null, "live_${nextId}")
                    // Wait for TTS to finish
                    delay(1500)
                    while (tts?.isSpeaking == true) delay(100)
                }
                phase = LiveVoicePhase.IDLE
            }
        }
    }

    // ── Start listening ───────────────────────────────────────────────────────
    fun startListening() {
        val recognizer = speechRecognizer ?: return
        if (phase == LiveVoicePhase.SPEAKING || phase == LiveVoicePhase.THINKING) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 650L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 450L)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                phase = LiveVoicePhase.LISTENING
                currentTranscript = ""
                heardVoiceInTurn = false
                forcedStopRequested = false
                lastVoiceAtMs = 0L
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                val now = SystemClock.elapsedRealtime()
                amplitude = if (rmsdB < 4.5f) {
                    0.08f
                } else {
                    heardVoiceInTurn = true
                    lastVoiceAtMs = now
                    ((rmsdB - 4.5f) / 8f).coerceIn(0.08f, 1f)
                }
                if (heardVoiceInTurn && !forcedStopRequested && now - lastVoiceAtMs > 650L) {
                    forcedStopRequested = true
                    try { recognizer.stopListening() } catch (_: Exception) {}
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val fallback = currentTranscript.trim()
                if (fallback.isNotBlank() && heardVoiceInTurn) {
                    callGemini(fallback)
                } else {
                    phase = LiveVoicePhase.IDLE
                    amplitude = 0f
                    currentTranscript = ""
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    currentTranscript = text
                    callGemini(text)
                } else {
                    phase = LiveVoicePhase.IDLE
                }
            }
            override fun onPartialResults(partial: Bundle?) {
                if (amplitude > 0.14f) {
                    currentTranscript = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    // ── Permission launcher ────────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(speechRecognizer, phase) {
        if (speechRecognizer == null || phase != LiveVoicePhase.IDLE) return@LaunchedEffect
        delay(180)
        val check = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (check == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (!micPermissionRequested) {
            micPermissionRequested = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun onOrbTapped() {
        when (phase) {
            LiveVoicePhase.IDLE -> {
                val check = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                if (check == PackageManager.PERMISSION_GRANTED) startListening()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            LiveVoicePhase.LISTENING -> {
                try { speechRecognizer?.stopListening() } catch (_: Exception) {}
            }
            else -> {}
        }
    }

    // ── End session handler ───────────────────────────────────────────────────
    fun endSession() {
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        tts?.stop()
        phase = LiveVoicePhase.ENDED
        if (turns.isNotEmpty()) {
            onSessionComplete(turns)
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            ChatPanel(
                state = state,
                onClose = { scope.launch { drawerState.close() } },
                onOpenSearch = { scope.launch { drawerState.close() } },
                onNewChat = { endSession(); onNewChat() },
                onSelectChat = { endSession(); onSelectChat(it) },
                onDeleteChat = onDeleteChat,
                onSettings = { scope.launch { drawerState.close() }; settingsOpen = true },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Gemini", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Chat history", modifier = Modifier.size(22.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Outlined.Settings, "Settings", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
                Box(Modifier.align(Alignment.Center).offset(y = (-28).dp), contentAlignment = Alignment.Center) {
                    Surface(onClick = { onOrbTapped() }, color = Color.Transparent, shape = CircleShape) {
                        LiveVoiceOrb(phase = phase, amplitude = amplitude, modifier = Modifier.size(280.dp))
                    }
                }
                Surface(
                    onClick = { endSession() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                ) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "End conversation")
                    }
                }
            }
        }
    }
    if (settingsOpen) SettingsDialog(state, onSaveSettings, onClose = { settingsOpen = false })
    return

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Close button top-right
        IconButton(
            onClick = { endSession() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "End conversation",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Title
        Text(
            "Live Voice",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        )

        // Central orb — tappable
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                onClick = { onOrbTapped() },
                color = Color.Transparent,
                shape = CircleShape,
            ) {
                LiveVoiceOrb(
                    phase = phase,
                    amplitude = amplitude,
                    modifier = Modifier.size(280.dp),
                )
            }
        }

        // Live transcript below orb
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = { endSession() },
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            ) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = "End conversation")
                }
            }
        }
    }
}
