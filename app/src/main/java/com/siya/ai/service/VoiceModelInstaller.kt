package com.siya.ai.service

import com.siya.ai.stt.SttModelSource
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Downloads the VAD/STT assets into app-private storage with resumable temp files. */
class VoiceModelInstaller(private val vadStore: VadModelStore, private val sttStore: SttModelStore) {
    suspend fun downloadVad(onProgress: (Long, Long) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        downloadFile(VAD_URL, vadStore.modelFile, MIN_VAD_BYTES, onProgress)
        check(vadStore.isInstalled()) { "Silero VAD model validation failed" }
    }

    suspend fun downloadHindiStt(onProgress: (Long, Long) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        downloadFile(SttModelSource.MODEL_URL, sttStore.modelFile, MIN_STT_MODEL_BYTES, onProgress)
        downloadFile(SttModelSource.TOKENS_URL, sttStore.tokensFile, MIN_STT_TOKENS_BYTES, onProgress)
        sttStore.validate().getOrThrow()
    }

    private fun downloadFile(url: String, target: File, minimumBytes: Long, onProgress: (Long, Long) -> Unit) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.download")
        var existing = if (temp.isFile) temp.length() else 0L
        var connection: HttpURLConnection? = null
        try {
            connection = open(url, existing)
            connection.connect()
            var code = connection.responseCode
            if (existing > 0L && code == HttpURLConnection.HTTP_REQUESTED_RANGE_NOT_SATISFIABLE) {
                connection.disconnect()
                temp.delete()
                existing = 0L
                connection = open(url, 0L)
                connection.connect()
                code = connection.responseCode
            }
            require(code in 200..299) { "Download failed: HTTP $code" }
            val resumed = existing > 0L && code == HttpURLConnection.HTTP_PARTIAL
            if (!resumed) {
                existing = 0L
                temp.delete()
            }
            val remaining = connection.contentLengthLong
            val total = if (remaining > 0L) existing + remaining else -1L
            var downloaded = existing
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(temp, resumed).use { output ->
                    val buffer = ByteArray(512 * 1024)
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
            require(temp.length() >= minimumBytes) { "Downloaded file is incomplete: ${target.name}" }
            check(temp.renameTo(target)) { "Could not install ${target.name} atomically" }
        } finally {
            connection?.disconnect()
            if (temp.exists() && !target.isFile) temp.delete()
        }
    }

    private fun open(url: String, offset: Long): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            if (offset > 0L) setRequestProperty("Range", "bytes=$offset-")
        }

    companion object {
        const val VAD_URL = "https://huggingface.co/onnx-community/silero-vad/resolve/main/onnx/model.onnx"
        const val MIN_VAD_BYTES = 500_000L
        const val MIN_STT_MODEL_BYTES = 100_000_000L
        const val MIN_STT_TOKENS_BYTES = 10_000L
    }
}
