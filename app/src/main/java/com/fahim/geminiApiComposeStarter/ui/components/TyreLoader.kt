package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun TyreLoader(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TyreRotationTransition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TyreRotationAngle"
    )

    Image(
        painter = painterResource(id = R.drawable.f1_tyre),
        contentDescription = "Loading",
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .graphicsLayer { rotationZ = angle }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
fun TyreLoaderPreview() {
    GeminiApiComposeStarterTheme {
        TyreLoader(size = 36.dp)
    }
}
