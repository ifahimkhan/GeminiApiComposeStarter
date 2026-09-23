package com.fahim.geminiApiComposeStarter.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlin.math.max

object ImageUtils {

    /**
     * Loads a Bitmap from a content Uri and resizes it to a safe maximum dimension
     * (default 1024px) to prevent payload size errors and OutOfMemoryExceptions.
     */
    fun loadAndResizeBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            resizeToMax(original, maxDimension)
        } catch (_: Exception) {
            null
        }
    }

    fun resizeToMax(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxSide
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
