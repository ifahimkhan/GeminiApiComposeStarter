package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import com.fahim.geminiApiComposeStarter.data.Category
import com.fahim.geminiApiComposeStarter.ui.theme.*
import kotlin.math.*

@Composable
fun ReferenceBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(Color.Black)) {
        drawRect(Brush.radialGradient(listOf(Color(0xFF00265F), Color(0xFF000814), Color.Black), center = Offset(size.width * .1f, 0f), radius = size.height * .9f))
        val bandWidth = size.width / 6
        for (i in 0..6) {
            val x = i * bandWidth
            drawRect(Brush.horizontalGradient(listOf(Color(0x00000000), Color(0xAA263948), Color(0x12000000)), startX = x, endX = x + bandWidth), topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size(bandWidth, size.height))
        }
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .65f to Color(0x40000000), 1f to Color.Black))
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, radius: Dp = 16.dp, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(Brush.linearGradient(listOf(Color(0x22273545), Color(0xB20B0E14), Color(0x663F4452))))
        .border(0.7.dp, Brush.linearGradient(listOf(Color(0xAA929BA7), Color(0x11424B5E), Color(0x995E6570))), RoundedCornerShape(radius)), content = content)
}

@Composable
fun RoundControl(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(50.dp).clip(CircleShape)
        .background(Brush.linearGradient(listOf(Color(0x55295779), Color(0xCC030609))))
        .border(.6.dp, Brush.linearGradient(listOf(Color(0xCC94A5C1), Color.Transparent, Color(0x445F6E87))), CircleShape)) {
        Icon(icon, label, modifier = Modifier.size(23.dp), tint = if (enabled) Color.White else MutedText.copy(alpha = .4f))
    }
}
private val Hexagon = GenericShape { size, _ ->
    moveTo(size.width * .5f, 0f); lineTo(size.width * .94f, size.height * .25f)
    lineTo(size.width * .94f, size.height * .75f); lineTo(size.width * .5f, size.height)
    lineTo(size.width * .06f, size.height * .75f); lineTo(size.width * .06f, size.height * .25f); close()
}
@Composable
fun HexMicButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, diameter: Dp = 80.dp) {
    Box(modifier.size(diameter + 16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(ElectricBlue.copy(alpha = .33f), Color.Transparent))))
        Box(Modifier.size(diameter).clip(Hexagon).background(Brush.linearGradient(listOf(Color(0xFF79B7FF), ElectricBlue, Color(0xFF0062FF))))
            .border(.8.dp, Brush.verticalGradient(listOf(Color(0xFFC7DDFF), Color.Transparent)), Hexagon)
            .judyClickable(enabled = enabled, label = "Voice input", onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Mic, "Voice input", tint = Color.White, modifier = Modifier.size(35.dp))
        }
    }
}
@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier) {
    val icon = when (category) { Category.RESEARCH -> Icons.Outlined.Lightbulb; Category.HEALTH -> Icons.Outlined.FavoriteBorder; else -> Icons.Outlined.AutoAwesome }
    Box(modifier.size(42.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF1D6595), Color(0xFF081B2D))))
        .border(.6.dp, Color(0xFF557793), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, modifier = Modifier.size(23.dp), tint = Color(0xFFDFEDFF))
    }
}

