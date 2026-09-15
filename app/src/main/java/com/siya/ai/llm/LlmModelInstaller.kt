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
        val target = store.modelFile
        val temp = File(target.parentFile, "${target.name}.download")
        val connection = (URL(LlmModelStore.MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }

        try {
            connection.connect()
            require(connection.responseCode in 200..299) {
                "Model download failed: HTTP ${connection.responseCode}"
            }
            val total = connection.contentLengthLong
            var downloaded = 0L
            temp.delete()
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(temp).use { output ->
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
            require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) {
                "Downloaded Qwen model is incomplete"
            }
            require(sha256(temp).equals(LlmModelStore.MODEL_SHA256, ignoreCase = true)) {
                "Downloaded Qwen model SHA-256 mismatch"
            }
            check(temp.renameTo(target)) { "Could not install Qwen model atomically" }
            store.validate().getOrThrow()
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
