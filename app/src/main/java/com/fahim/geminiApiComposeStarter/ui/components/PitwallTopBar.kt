package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.ui.theme.F1Red
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.HairlineOutline
import com.fahim.geminiApiComposeStarter.ui.theme.PetronasTeal
import com.fahim.geminiApiComposeStarter.ui.theme.SurfaceDark
import com.fahim.geminiApiComposeStarter.ui.theme.TextPrimary
import com.fahim.geminiApiComposeStarter.ui.theme.TextSecondary

@Composable
fun PitwallTopBar(
    isLoading: Boolean,
    isError: Boolean,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = HairlineOutline),
        color = SurfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left 3dp teal vertical bar accent
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(PetronasTeal)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PITWALL",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "F1 INTELLIGENCE · GEMINI",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
            }

            // Status Indicator on the right
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    TyreLoader(size = 18.dp)
                } else if (isError) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(F1Red)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PetronasTeal)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(onClick = onClearChat) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Session",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun PitwallTopBarPreview() {
    GeminiApiComposeStarterTheme {
        PitwallTopBar(
            isLoading = false,
            isError = false,
            onClearChat = {}
        )
    }
}
