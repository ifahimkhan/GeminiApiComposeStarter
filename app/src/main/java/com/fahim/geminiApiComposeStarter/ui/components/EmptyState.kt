package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.HairlineOutline
import com.fahim.geminiApiComposeStarter.ui.theme.PetronasTeal
import com.fahim.geminiApiComposeStarter.ui.theme.SurfaceHigh
import com.fahim.geminiApiComposeStarter.ui.theme.TextPrimary
import com.fahim.geminiApiComposeStarter.ui.theme.TextSecondary

val SUGGESTION_CHIPS = listOf(
    "Explain DRS",
    "Tyre strategy basics",
    "Greatest overtakes",
    "How is a Grand Prix scored?"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyState(
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RADIO CHECK. ASK ME ANYTHING.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PITWALL RACE TELEMETRY & STRATEGY ASSISTANT",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            SUGGESTION_CHIPS.forEach { chipText ->
                SuggestionChipItem(
                    text = chipText,
                    onClick = { onChipClick(chipText) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionChipItem(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .border(width = 1.dp, color = HairlineOutline, shape = RoundedCornerShape(2.dp))
            .clickable { onClick() },
        color = SurfaceHigh,
        shape = RoundedCornerShape(2.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = PetronasTeal,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun EmptyStatePreview() {
    GeminiApiComposeStarterTheme {
        EmptyState(onChipClick = {})
    }
}
