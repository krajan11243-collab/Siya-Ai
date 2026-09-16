package com.siya.ai.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class LlmModelInstaller(private val store: LlmModelStore) {
    suspend fun download(onProgress: (Long, Long) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        if (store.restoreFromShared()) {
            onProgress(store.modelFile.length(), store.modelFile.length())
            return@withContext
        }
        val target = store.modelFile
        val temp = File(target.parentFile, "${target.name}.download")
        var existing = if (temp.isFile) temp.length() else 0L
        var connection: HttpURLConnection? = null
        try {
            connection = open(existing)
            connection.connect()
            var code = connection.responseCode
            if (existing > 0L && code == 416) {
                connection.disconnect()
                temp.delete()
                existing = 0L
                connection = open(0L)
                connection.connect()
                code = connection.responseCode
            }
            require(code in 200..299) { "Model download failed: HTTP $code" }
            val resumed = existing > 0L && code == HttpURLConnection.HTTP_PARTIAL
            if (!resumed) { existing = 0L; temp.delete() }
            val remaining = connection.contentLengthLong
            val total = if (remaining > 0L) existing + remaining else -1L
            var downloaded = existing
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(temp, resumed).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                    output.fd.sync()
                }
            }
            require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "Downloaded Qwen model is incomplete" }
            require(sha256(temp).equals(LlmModelStore.MODEL_SHA256, ignoreCase = true)) { "Downloaded Qwen model SHA-256 mismatch" }
            check(temp.renameTo(target)) { "Could not install Qwen model atomically" }
            store.validate().getOrThrow()
            store.backupToShared()
        } finally {
            connection?.disconnect()
            if (temp.exists() && target.length() < LlmModelStore.MIN_MODEL_BYTES) temp.delete()
        }
    }

    private fun open(offset: Long): HttpURLConnection = (URL(LlmModelStore.MODEL_URL).openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 60_000
        requestMethod = "GET"
        instanceFollowRedirects = true
        if (offset > 0L) setRequestProperty("Range", "bytes=$offset-")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
