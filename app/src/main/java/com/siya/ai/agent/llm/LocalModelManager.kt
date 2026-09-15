package com.siya.ai.agent.llm

import android.content.Context
import java.io.File

/** Owns model files without embedding large binaries in the APK or Git repository. */
class LocalModelManager(context: Context) {
    private val modelDir: File = File(context.filesDir, "models/llm").apply { mkdirs() }

    fun qwenModel(): File = File(modelDir, "qwen2.5-1.5b-instruct-q4_k_m.gguf")

    fun hasQwenModel(): Boolean = qwenModel().let { it.isFile && it.length() > 1024L * 1024L }

    fun modelSizeBytes(): Long = qwenModel().takeIf { it.isFile }?.length() ?: 0L

    fun requiredPath(): String = qwenModel().absolutePath
}
