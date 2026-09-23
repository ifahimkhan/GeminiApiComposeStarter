package com.fahim.geminiApiComposeStarter.core.tools

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

data class ProcessedDocument(
    val fileName: String,
    val mimeType: String,
    val extractedText: String,
    val characterCount: Int,
    val lineCount: Int,
    val isTruncated: Boolean,
)

class DocumentProcessorTool {

    fun processDocument(context: Context, uri: Uri, maxChars: Int = 5000): ProcessedDocument? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "text/plain"
            val fileName = queryFileName(context, uri) ?: "study_document.txt"

            val stringBuilder = StringBuilder()
            var lineCount = 0
            var isTruncated = false

            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        lineCount++
                        if (stringBuilder.length + line.length < maxChars) {
                            stringBuilder.append(line).append("\n")
                        } else {
                            isTruncated = true
                            break
                        }
                        line = reader.readLine()
                    }
                }
            }

            val text = stringBuilder.toString().trim()
            ProcessedDocument(
                fileName = fileName,
                mimeType = mimeType,
                extractedText = text,
                characterCount = text.length,
                lineCount = lineCount,
                isTruncated = isTruncated,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}
