package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.fahim.geminiApiComposeStarter.R

/** Popup anchorBounds and popupContentSize are in the same window coordinate system. */
internal object AttachmentPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset(
        anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        (anchorBounds.bottom - popupContentSize.height).coerceAtLeast(0),
    )
}

@Composable
internal fun AttachmentPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPhotos: () -> Unit,
    onCamera: () -> Unit,
    onFiles: () -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    BackHandler(expanded, onDismiss)
    if (!visibility.currentState && !visibility.targetState) return

    val transition = updateTransition(visibility, label = "Attachment expansion")
    val progress by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 180 else 120, easing = FastOutSlowInEasing) },
        label = "Button to panel",
    ) { if (it) 1f else 0f }
    val configuration = LocalConfiguration.current
    val panelWidth = minOf(272.dp, (configuration.screenWidthDp - 32).coerceAtLeast(38).dp)
    val panelHeight = minOf(228.dp, (configuration.screenHeightDp - 80).coerceAtLeast(150).dp)
    val buttonSize = 38.dp
    Popup(
        popupPositionProvider = AttachmentPositionProvider,
        onDismissRequest = onDismiss,
        // Never take input focus: opening this panel does not change the keyboard state.
        properties = PopupProperties(focusable = false),
    ) {
        Box(Modifier.size(panelWidth, panelHeight)) {
            Surface(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 1f)
                    scaleX = buttonSize / panelWidth + (1f - buttonSize / panelWidth) * progress
                    scaleY = buttonSize / panelHeight + (1f - buttonSize / panelHeight) * progress
                },
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF191A1C),
                contentColor = Color(0xFFF4F4F5),
                border = BorderStroke(1.dp, Color(0xFF38393B)),
                shadowElevation = 6.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 12.dp).graphicsLayer {
                        alpha = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    },
                ) {
                    AttachmentAction("Camera", R.drawable.ic_attachment_camera, onCamera, Modifier.weight(1f), expanded)
                    AttachmentAction("Photos", R.drawable.ic_attachment_photos, onPhotos, Modifier.weight(1f), expanded)
                    AttachmentAction("Files", R.drawable.ic_attachment_files, onFiles, Modifier.weight(1f), expanded)
                }
            }
            // The visible + dissolves at the same location as the original button.
            Icon(
                Icons.Default.Add, null,
                Modifier.align(Alignment.BottomStart).size(buttonSize).padding(8.dp)
                    .graphicsLayer { alpha = (1f - progress * 3f).coerceIn(0f, 1f) },
                tint = Color(0xFFECECEC),
            )
        }
    }
}

@Composable
private fun AttachmentAction(title: String, icon: Int, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(44.dp).background(Color(0xFF292A2C), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(24.dp))
        }
        Text(title, fontSize = 18.sp)
    }
}
