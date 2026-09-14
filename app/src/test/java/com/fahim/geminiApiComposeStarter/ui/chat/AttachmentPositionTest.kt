package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.unit.*
import org.junit.Assert.assertEquals
import org.junit.Test

class AttachmentPositionTest {
    @Test fun panelOverlapsEntireButtonAndTracksKeyboardResize() {
        for (buttonTop in listOf(740, 420)) {
            val anchor = IntRect(18, buttonTop, 56, buttonTop + 38)
            val panel = IntSize(272, 228)
            val position = AttachmentPositionProvider.calculatePosition(
                anchor, IntSize(393, 851), LayoutDirection.Ltr, panel,
            )
            assertEquals(anchor.left, position.x)
            assertEquals(anchor.bottom, position.y + panel.height)
        }
    }
}
