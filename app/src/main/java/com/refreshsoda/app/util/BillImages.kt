package com.refreshsoda.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/** Keeps supplier bill photos inside the app so they stay readable after the gallery permission expires. */
object BillImages {
    /** Copies the picked image into app storage and returns its absolute path (null if copying failed). */
    fun copyToAppStorage(context: Context, source: Uri): String? {
        return try {
            val dir = File(context.filesDir, "bills").apply { mkdirs() }
            val target = File(dir, "bill_${UUID.randomUUID()}.jpg")
            val input = context.contentResolver.openInputStream(source) ?: return null
            input.use { stream -> target.outputStream().use { out -> stream.copyTo(out) } }
            target.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Decodes a downsampled copy so large photos do not exhaust memory. */
    fun loadBitmap(path: String, maxSide: Int = 1400): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
