package com.fahim.geminiApiComposeStarter.ui.components

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val CrownIcon: ImageVector = ImageVector.Builder("Crown", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(3f, 7f); lineTo(5f, 19f); quadTo(12f, 21f, 19f, 19f); lineTo(21f, 7f)
        lineTo(16f, 12f); lineTo(12f, 5f); lineTo(8f, 12f); close()
        moveTo(6f, 16f); quadTo(12f, 18f, 18f, 16f)
    }
}.build()
val NewChatIcon: ImageVector = ImageVector.Builder("NewChat", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 3f); lineTo(7f, 3f); quadTo(3f, 3f, 3f, 7f); lineTo(3f, 17f)
        quadTo(3f, 21f, 7f, 21f); lineTo(17f, 21f); quadTo(21f, 21f, 21f, 17f); lineTo(21f, 12f)
        moveTo(9f, 15f); lineTo(10f, 10f); lineTo(18f, 2f); quadTo(22f, 1f, 22f, 6f); lineTo(14f, 14f); close()
        moveTo(16f, 4f); lineTo(20f, 8f)
    }
}.build()
