package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

val LocalReducedMotion = staticCompositionLocalOf { false }

/** A small tactile response shared by the glass cards and microphone. */
fun Modifier.judyClickable(enabled: Boolean = true, label: String? = null, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if (pressed && !reduced) .96f else 1f,
        if (reduced) snap() else spring(dampingRatio = .8f, stiffness = 650f), label = "press")
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClickLabel = label, role = Role.Button, onClick = onClick)
}
