package com.fahim.geminiApiComposeStarter.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Shows a picture from app storage. It is decoded at a reduced size on a background thread, so long
 * chats with many pictures stay smooth. Give [modifier] a fixed size so the layout does not jump when
 * the picture appears.
 */
@Composable
fun StoredImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxSidePx: Int = 640,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, maxSidePx) {
        value = withContext(Dispatchers.IO) { decodeSampled(path, maxSidePx)?.asImageBitmap() }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

private fun decodeSampled(path: String, maxSidePx: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val longest = max(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) return null

    var sample = 1
    while (longest / (sample * 2) >= maxSidePx) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
