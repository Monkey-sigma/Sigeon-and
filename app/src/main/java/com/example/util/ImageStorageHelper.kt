package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {

    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val imagesDir = File(context.filesDir, "user_images").apply {
                    if (!exists()) mkdirs()
                }
                val extension = context.contentResolver.getType(sourceUri)?.let { mime ->
                    when {
                        mime.contains("png") -> "png"
                        mime.contains("webp") -> "webp"
                        mime.contains("gif") -> "gif"
                        else -> "jpg"
                    }
                } ?: "jpg"

                val destFile = File(imagesDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension")
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                destFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun deleteImage(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists() && file.absolutePath.contains("user_images")) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Safely decodes and downsamples a bitmap for Widget or preview use without OOM.
     */
    fun loadScaledBitmap(filePath: String?, maxDim: Int = 600): Bitmap? {
        if (filePath.isNullOrBlank()) return null
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while (origWidth / sampleSize > maxDim || origHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
