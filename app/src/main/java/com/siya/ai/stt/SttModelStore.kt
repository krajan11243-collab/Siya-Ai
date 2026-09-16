package com.siya.ai.stt

import android.content.Context
import com.siya.ai.storage.SharedModelBackup
import java.io.File

/** Local ASR model storage with a user-visible shared backup. */
class SttModelStore(context: Context) {
    companion object {
        const val LANGUAGE_HI = "hi"
        const val MODEL_FILE = "model.int8.onnx"
        const val TOKENS_FILE = "tokens.txt"
        private const val MIN_MODEL_BYTES = 100_000_000L
        private const val MIN_TOKENS_BYTES = 10_000L
    }

    private val legacyRoot = File(context.filesDir, "models/asr/$LANGUAGE_HI")
    private val externalRoot = File(
        context.getExternalFilesDir(null) ?: context.filesDir,
        "SiyaAi/Models/ASR/$LANGUAGE_HI"
    )
    private val root = if (File(legacyRoot, MODEL_FILE).isFile) legacyRoot else externalRoot
        .apply { mkdirs() }
    private val sharedBackup = SharedModelBackup(context)

    val modelFile: File get() = File(root, MODEL_FILE)
    val tokensFile: File get() = File(root, TOKENS_FILE)

    fun isInstalled(): Boolean = validate().isSuccess

    fun validate(): Result<Unit> = runCatching {
        require(modelFile.isFile) { "Hindi STT model is missing" }
        require(modelFile.length() >= MIN_MODEL_BYTES) { "Hindi STT model is incomplete" }
        require(tokensFile.isFile) { "Hindi STT tokens.txt is missing" }
        require(tokensFile.length() >= MIN_TOKENS_BYTES) { "Hindi STT tokens file is incomplete" }
    }

    fun restoreFromShared(): Boolean {
        if (isInstalled()) return true
        val modelOk = sharedBackup.restoreTo(MODEL_FILE, SharedModelBackup.ASR_PATH, modelFile, MIN_MODEL_BYTES)
        val tokensOk = sharedBackup.restoreTo(TOKENS_FILE, SharedModelBackup.ASR_PATH, tokensFile, MIN_TOKENS_BYTES)
        return modelOk && tokensOk && isInstalled()
    }

    fun backupToShared(): Boolean =
        sharedBackup.copyToShared(modelFile, SharedModelBackup.ASR_PATH) &&
            sharedBackup.copyToShared(tokensFile, SharedModelBackup.ASR_PATH)

    fun modelPath(): String = modelFile.absolutePath
    fun tokensPath(): String = tokensFile.absolutePath
    fun modelDirectory(): File = root
}
