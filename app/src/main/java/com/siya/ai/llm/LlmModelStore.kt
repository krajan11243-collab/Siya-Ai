package com.siya.ai.llm

import android.content.Context
import com.siya.ai.storage.SharedModelBackup
import java.io.File

class LlmModelStore(context: Context) {
    companion object {
        const val MODEL_FILE = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        const val MODEL_URL =
            "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
        const val MODEL_SHA256 =
            "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e"
        const val MIN_MODEL_BYTES = 1_000_000_000L
    }

    val appContext: Context = context.applicationContext
    private val legacyRoot = File(context.filesDir, "models/llm/qwen2.5-1.5b")
    private val externalRoot = File(
        context.getExternalFilesDir(null) ?: context.filesDir,
        "SiyaAi/Models/LLM/qwen2.5-1.5b"
    )
    private val root = if (File(legacyRoot, MODEL_FILE).isFile) legacyRoot else externalRoot
        .apply { mkdirs() }
    private val sharedBackup = SharedModelBackup(context)

    val modelFile: File get() = File(root, MODEL_FILE)

    fun isInstalled(): Boolean = validate().isSuccess

    fun validate(): Result<Unit> = runCatching {
        require(modelFile.isFile) { "Qwen GGUF model is missing" }
        require(modelFile.length() >= MIN_MODEL_BYTES) { "Qwen GGUF model is incomplete" }
    }

    suspend fun restoreFromShared(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (isInstalled()) return@withContext true
        sharedBackup.restoreTo(MODEL_FILE, SharedModelBackup.LLM_PATH, modelFile, MIN_MODEL_BYTES)
    }

    fun backupToShared(): Boolean = sharedBackup.copyToShared(modelFile, SharedModelBackup.LLM_PATH)

    fun modelPath(): String = modelFile.absolutePath
    fun modelDirectory(): File = root
}
