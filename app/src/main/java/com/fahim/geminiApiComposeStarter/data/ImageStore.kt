package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Keeps attached pictures in app storage so they outlive the picker or camera that produced them. */
interface ImageStore {
    /** Copies the image at [source] (a Uri string) into app storage, downsized. Returns the file path, or null on failure. */
    suspend fun import(source: String): String?

    suspend fun load(path: String): Bitmap?

    suspend fun delete(path: String)
}

private const val MAX_SIDE_PX = 1280
private const val JPEG_QUALITY = 85

class FileImageStore(context: Context) : ImageStore {

    private val appContext = context.applicationContext
    private val imageDir = File(appContext.filesDir, "images")

    override suspend fun import(source: String): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeUpright(Uri.parse(source)) ?: return@withContext null
            imageDir.mkdirs()
            val file = File(imageDir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            file.absolutePath
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun load(path: String): Bitmap? = withContext(Dispatchers.IO) {
        BitmapFactory.decodeFile(path)
    }

    override suspend fun delete(path: String) {
        withContext(Dispatchers.IO) { File(path).delete() }
    }

    /** Decodes [uri], scales it so its longest side is at most [MAX_SIDE_PX] and applies the EXIF rotation. */
    private fun decodeUpright(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
        val scale = min(1f, MAX_SIDE_PX.toFloat() / max(decoded.width, decoded.height))
        matrix.postScale(scale, scale)

        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (result !== decoded) decoded.recycle()
        return result
    }
}
