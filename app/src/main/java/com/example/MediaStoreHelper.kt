package com.example

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object MediaStoreHelper {
    fun saveVideoToPublicMovies(context: Context, sourceFile: File, originalFileName: String): Uri? {
        val rawBaseName = originalFileName.substringBeforeLast(".")
        val finalName = "${rawBaseName}_CFR_Fixed.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, finalName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SmoothScrollFixer")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val itemUri = resolver.insert(collectionUri, values) ?: return null

        try {
            resolver.openOutputStream(itemUri).use { outputStream ->
                if (outputStream == null) return null
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            return itemUri
        } catch (e: Exception) {
            try {
                resolver.delete(itemUri, null, null)
            } catch (ignored: Exception) {}
            return null
        }
    }
}
