package com.siya.ai.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Keeps model copies in user-visible Downloads/Siya Ai/Models so an uninstall or
 * app-data reset does not force a multi-GB model download again.
 * Android 10+ uses MediaStore, avoiding MANAGE_EXTERNAL_STORAGE.
 */
class SharedModelBackup(private val context: Context) {
    fun find(fileName: String, relativePath: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        resolver.query(collection, projection, selection, arrayOf(fileName, relativePath), null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val size = cursor.getLong(1)
                if (size > 0L) return Uri.withAppendedPath(collection, id.toString())
            }
        }
        return null
    }

    fun copyToShared(source: File, relativePath: String): Boolean {
        if (!source.isFile || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // Do not rewrite a valid existing backup on every app start/update.
        if (find(source.name, relativePath) != null) return true
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        return try {
            resolver.openOutputStream(uri, "w")!!.use { output ->
                source.inputStream().use { input -> input.copyTo(output, 1024 * 1024) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            false
        }
    }

    fun restoreTo(fileName: String, relativePath: String, target: File, minimumBytes: Long): Boolean {
        if (target.isFile && target.length() >= minimumBytes) return true
        val uri = find(fileName, relativePath) ?: return false
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.restore")
        return try {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                temp.outputStream().use { output -> input.copyTo(output, 1024 * 1024) }
            }
            if (temp.length() < minimumBytes) false else temp.renameTo(target)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    companion object {
        const val ROOT = "Download/Siya Ai/Models/"
        const val LLM_PATH = "Download/Siya Ai/Models/LLM/"
        const val VAD_PATH = "Download/Siya Ai/Models/VAD/"
        const val ASR_PATH = "Download/Siya Ai/Models/ASR/hi/"
    }
}
