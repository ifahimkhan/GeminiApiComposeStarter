package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.HairlineOutline
import com.fahim.geminiApiComposeStarter.ui.theme.PetronasTeal

@Composable
fun F1Backdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                // Faint carbon-fibre / thin-grid line pattern
                val gridSpacing = 24.dp.toPx()
                val lineAlpha = 0.03f
                val gridColor = HairlineOutline.copy(alpha = lineAlpha)

                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }

                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }
            }
    ) {
        // F1 logo centered backdrop (70% width, alpha 0.06f, teal ColorFilter)
        Image(
            painter = painterResource(id = R.drawable.f1_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.Center),
            contentScale = ContentScale.Fit,
            alpha = 0.06f,
            colorFilter = ColorFilter.tint(PetronasTeal)
        )

        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun F1BackdropPreview() {
    GeminiApiComposeStarterTheme {
        F1Backdrop()
    }
}
