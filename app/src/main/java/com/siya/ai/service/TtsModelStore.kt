package com.siya.ai.service

import android.content.Context
import java.io.File

/** Local manifest for an optional Sherpa-ONNX Kokoro TTS model. */
class TtsModelStore(context: Context) {
    companion object {
        const val MODEL_DIR = "kokoro-multi-lang-v1_1"
        const val MODEL_VERSION = "kokoro-multi-lang-v1_1 / sherpa-onnx-v1.13.8"
    }

    private val root = File(context.applicationContext.filesDir, "tts/$MODEL_DIR")

    fun directory(): File = root
    fun modelPath(): File = File(root, "model.onnx")
    fun voicesPath(): File = File(root, "voices.bin")
    fun tokensPath(): File = File(root, "tokens.txt")
    fun dataDir(): File = File(root, "espeak-ng-data")

    /** Lexicon is optional for the multi-language backend; model validation requires core files only. */
    fun isInstalled(): Boolean =
        modelPath().isFile && modelPath().length() > 1_000_000L &&
            voicesPath().isFile && voicesPath().length() > 100_000L &&
            tokensPath().isFile && tokensPath().length() > 100L &&
            dataDir().isDirectory

    fun validate(): Result<Unit> = runCatching {
        require(isInstalled()) {
            "Kokoro TTS model is missing or incomplete. Import $MODEL_DIR into ${root.parentFile}"
        }
    }

    fun delete() {
        root.deleteRecursively()
    }
}
