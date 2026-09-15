package com.siya.ai.stt

import android.content.Context
import java.io.File

/**
 * Local-only ASR model storage.
 *
 * Expected layout:
 * files/models/asr/hi/model.int8.onnx
 * files/models/asr/hi/tokens.txt
 */
class SttModelStore(context: Context) {
    companion object {
        const val LANGUAGE_HI = "hi"
        const val MODEL_FILE = "model.int8.onnx"
        const val TOKENS_FILE = "tokens.txt"
        private const val MIN_MODEL_BYTES = 100_000_000L
        private const val MIN_TOKENS_BYTES = 10_000L
    }

    private val root = File(context.filesDir, "models/asr/$LANGUAGE_HI").apply { mkdirs() }

    val modelFile: File get() = File(root, MODEL_FILE)
    val tokensFile: File get() = File(root, TOKENS_FILE)

    fun isInstalled(): Boolean = validate().isSuccess

    fun validate(): Result<Unit> = runCatching {
        require(modelFile.isFile) { "Hindi STT model is missing" }
        require(modelFile.length() >= MIN_MODEL_BYTES) { "Hindi STT model is incomplete" }
        require(tokensFile.isFile) { "Hindi STT tokens.txt is missing" }
        require(tokensFile.length() >= MIN_TOKENS_BYTES) { "Hindi STT tokens file is incomplete" }
    }

    fun modelPath(): String = modelFile.absolutePath
    fun tokensPath(): String = tokensFile.absolutePath
}
