package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.HairlineOutline
import com.fahim.geminiApiComposeStarter.ui.theme.PetronasTeal
import com.fahim.geminiApiComposeStarter.ui.theme.SilverSecondary
import com.fahim.geminiApiComposeStarter.ui.theme.SurfaceDark
import com.fahim.geminiApiComposeStarter.ui.theme.SurfaceHigh
import com.fahim.geminiApiComposeStarter.ui.theme.TextPrimary
import com.fahim.geminiApiComposeStarter.ui.theme.TextSecondary

@Composable
fun ChatBubble(
    isUser: Boolean,
    text: String,
    timestamp: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(120)) +
                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(120))
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Upper small mono label: "DRIVER" in teal for user, "PITWALL" in silver for bot
            Text(
                text = if (isUser) "DRIVER" else "PITWALL",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) PetronasTeal else SilverSecondary,
                modifier = Modifier.padding(bottom = 3.dp, start = 2.dp, end = 2.dp)
            )

            if (isUser) {
                // User Bubble: Right-aligned, Surface High, 1dp teal border, 2dp shape max
                Surface(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .border(width = 1.dp, color = PetronasTeal, shape = RoundedCornerShape(2.dp)),
                    color = SurfaceHigh,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            } else {
                // Gemini Bubble: Left-aligned, Surface, 1dp Outline border, 3dp teal left edge strip
                Surface(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .border(width = 1.dp, color = HairlineOutline, shape = RoundedCornerShape(2.dp)),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Row {
                        // 3dp teal left edge strip
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxWidth()
                                .background(PetronasTeal)
                        )
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TyreLoader(size = 22.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "ANALYSING TELEMETRY...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PetronasTeal,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = text.toBoldAnnotatedString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Tiny mono timestamp under each bubble
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun ChatBubbleUserPreview() {
    GeminiApiComposeStarterTheme {
        ChatBubble(
            isUser = true,
            text = "Copy that pit wall, how are my tyres looking?",
            timestamp = "14:32"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun ChatBubblePitwallPreview() {
    GeminiApiComposeStarterTheme {
        ChatBubble(
            isUser = false,
            text = "**BOX BOX.** Tyre deg is at 78%. Switching to Hard compound.",
            timestamp = "14:33"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun ChatBubbleLoadingPreview() {
    GeminiApiComposeStarterTheme {
        ChatBubble(
            isUser = false,
            text = "",
            timestamp = "14:34",
            isLoading = true
        )
    }
}
