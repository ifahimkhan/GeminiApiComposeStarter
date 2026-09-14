package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** Stores generated images only in the app's private, non-backed-up storage. */
class GeneratedImageStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "generated_images")

    fun save(id: Long, bytes: ByteArray, mimeType: String): String {
        directory.mkdirs()
        val extension = if (mimeType.equals("image/jpeg", ignoreCase = true)) "jpg" else "png"
        val target = AtomicFile(File(directory, "image_$id.$extension"))
        val output = target.startWrite()
        try {
            output.write(bytes)
            target.finishWrite(output)
        } catch (error: Exception) {
            target.failWrite(output)
            throw error
        }
        return target.baseFile.absolutePath
    }
}
