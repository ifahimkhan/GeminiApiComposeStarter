package com.fahim.geminiApiComposeStarter.ui.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole

@Composable
fun LiveVoiceDialog(
    voiceState: VoiceState,
    isSpeaking: Boolean,
    recentMessages: List<ChatMessage>,
    handsFreeEnabled: Boolean,
    onToggleHandsFree: () -> Unit,
    onVoiceInputReceived: (String) -> Unit,
    onInterrupt: () -> Unit,
    onDismiss: () -> Unit,
) {
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onVoiceInputReceived(spokenText)
            }
        }
    }

    fun launchVoiceInput() {
        onInterrupt()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: Exception) {
            // Speech recognizer not available
        }
    }

    Dialog(
        onDismissRequest = {
            onInterrupt()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Voice Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (handsFreeEnabled) "Hands-Free: Active" else "Push-to-Talk",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onInterrupt()
                            onDismiss()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Transcript Preview Area
                val transcript = remember(recentMessages) {
                    recentMessages.takeLast(4)
                }
                val listState = rememberLazyListState()

                LaunchedEffect(transcript.size) {
                    if (transcript.isNotEmpty()) {
                        listState.animateScrollToItem(transcript.size - 1)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (transcript.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Tap the microphone below to start a live voice conversation with Gemini.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(transcript, key = { it.id }) { msg ->
                                val isUser = msg.role == MessageRole.USER
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                    ) {
                                        Text(
                                            text = msg.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Animated Pulsating Orb Visualizer
                VoiceOrbVisualizer(
                    voiceState = voiceState,
                    isSpeaking = isSpeaking,
                    onOrbClick = {
                        if (isSpeaking) {
                            onInterrupt()
                        } else {
                            launchVoiceInput()
                        }
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status text indicator
                val statusText = when {
                    isSpeaking -> "Gemini speaking... (Tap to interrupt)"
                    voiceState == VoiceState.LISTENING -> "Listening to your voice..."
                    voiceState == VoiceState.THINKING -> "Gemini is thinking..."
                    else -> "Tap the microphone to speak"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isSpeaking -> MaterialTheme.colorScheme.primary
                        voiceState == VoiceState.LISTENING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Hands-Free Toggle
                    TextButton(
                        onClick = onToggleHandsFree,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = if (handsFreeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (handsFreeEnabled) "Auto-Reply: ON" else "Auto-Reply: OFF",
                            color = if (handsFreeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Primary Action Button: Mic or Stop
                    if (isSpeaking) {
                        FilledIconButton(
                            onClick = onInterrupt,
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Interrupt",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onError,
                            )
                        }
                    } else {
                        FilledIconButton(
                            onClick = { launchVoiceInput() },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun VoiceOrbVisualizer(
    voiceState: VoiceState,
    isSpeaking: Boolean,
    onOrbClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking || voiceState == VoiceState.LISTENING) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 600 else if (voiceState == VoiceState.LISTENING) 800 else 1800,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waveAlpha",
    )

    val orbGradient = when {
        isSpeaking -> Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondary,
            ),
        )
        voiceState == VoiceState.LISTENING -> Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.tertiaryContainer,
                Color(0xFF00E5FF),
            ),
        )
        voiceState == VoiceState.THINKING -> Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.primary,
            ),
        )
        else -> Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp),
    ) {
        // Outer animated ripple ring
        if (isSpeaking || voiceState == VoiceState.LISTENING) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = waveAlpha),
                    ),
            )
        }

        // Inner core orb
        Surface(
            onClick = onOrbClick,
            shape = CircleShape,
            modifier = Modifier
                .size(110.dp)
                .scale(if (voiceState == VoiceState.IDLE) 1f else pulseScale * 0.9f),
            color = Color.Transparent,
            shadowElevation = 8.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(orbGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = if (isSpeaking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
