package com.fahim.geminiApiComposeStarter.ui.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.LocalMascotGlow
import kotlin.math.PI
import kotlin.math.sin

/**
 * Per-state motion spec.
 *
 * [periodMillis], [bobDp], [tiltDegrees] and the scale range describe the *looping* part of the
 * animation; [restTiltDegrees], [restScale], [glowAlpha] and [bodyAlpha] are the *settled* values
 * the character springs to when it enters the state. Splitting the two is what makes a state
 * change read as the character reacting rather than as the animation jumping.
 */
private data class MascotMotion(
    val periodMillis: Int,
    val bobDp: Float,
    val tiltDegrees: Float,
    val pulseScale: Float,
    val restTiltDegrees: Float,
    val restScale: Float,
    val glowAlpha: Float,
    val bodyAlpha: Float = 1f,
)

private fun MascotState.motion(): MascotMotion = when (this) {
    // Matches the supplied idle Lottie: 2s loop, +/-1.5 degrees, +/-3px bob, 98-100% scale.
    MascotState.IDLE -> MascotMotion(2000, 3f, 1.5f, 0.02f, 0f, 1f, 0.32f)
    MascotState.THINKING -> MascotMotion(900, 2f, 4f, 0.025f, -8f, 1f, 0.78f)
    MascotState.HAPPY -> MascotMotion(700, 5f, 2f, 0.03f, 0f, 1.04f, 0.58f)
    MascotState.EXCITED -> MascotMotion(450, 7f, 7f, 0.04f, 0f, 1.06f, 0.82f)
    MascotState.CONFUSED -> MascotMotion(1600, 1.5f, 3f, 0.015f, 12f, 0.98f, 0.40f)
    MascotState.SURPRISED -> MascotMotion(600, 2f, 0f, 0.05f, 0f, 1.10f, 0.90f)
    MascotState.SLEEPY -> MascotMotion(3600, 4f, 3f, 0.015f, 7f, 0.94f, 0.14f, bodyAlpha = 0.72f)
}

@Composable
private fun MascotState.description(): String = stringResource(
    when (this) {
        MascotState.IDLE -> R.string.mascot_idle
        MascotState.THINKING -> R.string.mascot_thinking
        MascotState.HAPPY -> R.string.mascot_happy
        MascotState.EXCITED -> R.string.mascot_excited
        MascotState.CONFUSED -> R.string.mascot_confused
        MascotState.SURPRISED -> R.string.mascot_surprised
        MascotState.SLEEPY -> R.string.mascot_sleepy
    },
)

/**
 * The AhumLabs assistant character.
 *
 * Deliberately *not* a Lottie view. The supplied `.json` is a single embedded-PNG image layer
 * with a transform-only loop, so a Lottie runtime would add a dependency and a JSON parse to
 * replay a transform Compose can drive itself. Driving it here means every animated value is
 * written inside `graphicsLayer`'s lambda block, which runs in the draw phase -- the composable
 * never recomposes while animating, and the whole avatar is one texture upload.
 */
@Composable
fun MascotAvatar(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val motion = remember(state) { state.motion() }
    val glowColor = LocalMascotGlow.current
    val density = LocalDensity.current

    // One looping driver for the whole avatar. Everything else is derived from its phase.
    val transition = rememberInfiniteTransition(label = "mascot-loop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(motion.periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mascot-phase",
    )

    // Settled values spring on a state change, so entering THINKING looks like the character
    // leaning in rather than the loop restarting.
    val restTilt by animateFloatAsState(
        targetValue = motion.restTiltDegrees,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mascot-rest-tilt",
    )
    val restScale by animateFloatAsState(
        targetValue = motion.restScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mascot-rest-scale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = motion.glowAlpha,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "mascot-glow",
    )
    val bodyAlpha by animateFloatAsState(
        targetValue = motion.bodyAlpha,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "mascot-body-alpha",
    )

    val description = state.description()

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // Glow halo behind the character, tinted with its eye colour.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val wave = sin(phase * 2f * PI.toFloat())
                    val pulse = 1f + wave * 0.06f
                    scaleX = pulse
                    scaleY = pulse
                    alpha = glowAlpha * (0.8f + wave * 0.2f)
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, glowColor.copy(alpha = 0f)),
                            radius = this.size.minDimension / 2f,
                        ),
                    )
                },
        )

        Image(
            painter = painterResource(R.drawable.mascot_ahum),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val wave = sin(phase * 2f * PI.toFloat())
                    val scale = restScale + wave * motion.pulseScale
                    translationY = with(density) { (-wave * motion.bobDp).dp.toPx() }
                    rotationZ = restTilt + wave * motion.tiltDegrees
                    scaleX = scale
                    scaleY = scale
                    alpha = bodyAlpha
                },
        )
    }
}

@Preview(showBackground = true, name = "Mascot states - light")
@Composable
private fun MascotAvatarPreview() {
    GeminiApiComposeStarterTheme {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MascotState.entries.forEach { MascotAvatar(state = it, size = 56.dp) }
        }
    }
}

@Preview(showBackground = true, name = "Mascot states - dark", backgroundColor = 0xFF101415)
@Composable
private fun MascotAvatarDarkPreview() {
    GeminiApiComposeStarterTheme(themeMode = com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode.DARK) {
        Box(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MascotState.entries.forEach { MascotAvatar(state = it, size = 56.dp) }
            }
        }
    }
}

@Preview(showBackground = true, name = "Mascot - thinking, large")
@Composable
private fun MascotAvatarThinkingPreview() {
    GeminiApiComposeStarterTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MascotAvatar(state = MascotState.THINKING, size = 120.dp)
        }
    }
}
