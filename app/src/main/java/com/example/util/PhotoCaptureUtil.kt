package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PhotoCaptureUtil {

    private const val PHOTO_DIR_NAME = "visitor_photos"

    /**
     * Saves a captured visitor camera bitmap into internal app storage and returns the local file path.
     */
    fun saveVisitorPhoto(context: Context, bitmap: Bitmap): String? {
        return try {
            val photosDir = File(context.filesDir, PHOTO_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            val fileName = "visitor_photo_${System.currentTimeMillis()}.jpg"
            val photoFile = File(photosDir, fileName)

            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            photoFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a Bitmap from the given local file path, or null if invalid.
     */
    fun loadVisitorPhoto(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
