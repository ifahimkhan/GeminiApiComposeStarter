package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/** Normalized geometry, shared across visits. Only the frame index changes at draw time.
 * ~11.8 MB for a full seamless cycle; no trigonometry or particle allocation per rendered frame. */
private object OrbGeometry {
    const val frameCount = 240
    private data class Particle(val angle: Float, val tube: Float, val depth: Float, val group: Int)
    private val particles = (0 until 32).flatMap { ring ->
        val tubeAngle = ring / 32f * 2f * PI.toFloat()
        (0 until 192).map { step ->
            val angle = step / 192f * 2f * PI.toFloat()
            val hue = (((sin(angle) + cos(angle)) * .32f + .5f) * 7).toInt().coerceIn(0, 7)
            Particle(angle, cos(tubeAngle), sin(tubeAngle), hue * 2 + if (sin(tubeAngle) > 0f) 1 else 0)
        }
    }.sortedBy { it.group }
    val counts = IntArray(16) { group -> particles.count { it.group == group } * 2 }
    val offsets = IntArray(16).also { for (i in 1..15) it[i] = it[i - 1] + counts[i - 1] }
    fun frame(index: Int): FloatArray {
        val phase = index / frameCount.toFloat() * 2f * PI.toFloat()
        return FloatArray(particles.size * 2).also { points ->
            particles.forEachIndexed { i, p ->
                val fold = sin(p.angle * 4f + phase) * .07f + cos(p.angle * 7f - phase) * .025f
                val radius = .34f * (.88f + .25f * p.tube + fold)
                points[i * 2] = .5f + cos(p.angle) * radius + p.depth * .027f * sin(phase + p.angle * 2)
                points[i * 2 + 1] = .5f + sin(p.angle) * radius + p.depth * .040f * cos(p.angle * 2 - phase)
            }
        }
    }
    val still by lazy { frame(0) }
    val frames by lazy { Array(frameCount) { frame(it) } }
}

@Composable
fun VoiceOrb(reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val frames by produceState<Array<FloatArray>?>(null, reducedMotion) {
        if (!reducedMotion) value = withContext(Dispatchers.Default) { OrbGeometry.frames }
    }
    val phase: State<Float> = if (reducedMotion) remember { mutableFloatStateOf(0f) } else
        rememberInfiniteTransition(label = "orb").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "orb cycle")
    val paints = remember { Array(16) { index -> android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = lerp(Color(0xFF067AFF), Color(0xFFBD6DE6), (index / 2) / 7f).copy(alpha = if (index % 2 == 0) .50f else .94f).toArgb()
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeWidth = .0018f
    } } }
    Canvas(modifier.aspectRatio(1f).drawWithCache {
        val center = Offset(size.width / 2, size.height / 2)
        val halo = Brush.radialGradient(listOf(Color(0x62012977), Color(0x30260061), Color.Transparent), center, size.minDimension * .5f)
        onDrawBehind { drawCircle(halo, size.minDimension * .5f, center) }
    }) {
        val progress = phase.value
        val points = if (reducedMotion) OrbGeometry.still else frames?.get((progress * OrbGeometry.frameCount).toInt().coerceIn(0, OrbGeometry.frameCount - 1)) ?: OrbGeometry.still
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.scale(size.width, size.height)
            for (group in paints.indices) native.drawPoints(points, OrbGeometry.offsets[group], OrbGeometry.counts[group], paints[group])
            native.restore()
        }
        val center = Offset(size.width / 2, size.height / 2)
        for (i in -3..3) {
            val base = when (abs(i)) { 0 -> .19f; 1 -> .14f; 2 -> .10f; else -> .045f }
            val pulse = if (reducedMotion) 1f else .88f + .12f * sin(progress * 4f * PI.toFloat() + i)
            val h = size.height * base * pulse
            val x = center.x + i * size.width * .042f
            drawLine(Color(0x2053BEFF), Offset(x, center.y - h / 2), Offset(x, center.y + h / 2), strokeWidth = size.width * .024f, cap = StrokeCap.Round)
            drawLine(Color(0xFFE8F3FF), Offset(x, center.y - h / 2), Offset(x, center.y + h / 2), strokeWidth = size.width * .010f, cap = StrokeCap.Round)
        }
    }
}
