package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import kotlin.math.max

data class ChatAttachment(
    val uriString: String,
    val name: String,
    val isImage: Boolean,
    val mimeType: String = "",
)

object AttachmentUtils {

    fun createTempCameraUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val imageFile = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "attachment"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }

    fun isImageUri(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri) ?: ""
        if (type.startsWith("image/")) return true
        val name = getFileName(context, uri).lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
            name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp")
    }

    fun loadScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            var inSampleSize = 1
            val maxEdge = max(options.outWidth, options.outHeight)
            while (maxEdge / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun readFileText(context: Context, uri: Uri, maxChars: Int = 12000): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { reader ->
                    val chars = CharArray(maxChars)
                    val read = reader.read(chars, 0, maxChars)
                    if (read > 0) String(chars, 0, read) else ""
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
