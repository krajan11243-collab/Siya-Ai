package com.siya.ai.vad

import android.content.Context
import java.io.File

class VadModelStore(context: Context) {
    companion object { const val FILE_NAME = "silero_vad_v5.onnx" }

    private val modelDir = File(context.filesDir, "models/vad").apply { mkdirs() }
    val modelFile: File get() = File(modelDir, FILE_NAME)

    fun isInstalled(): Boolean = modelFile.isFile && modelFile.length() > 100_000L
    fun readBytes(): ByteArray = require(isInstalled()) { "Silero VAD model is not installed" }.let { modelFile.readBytes() }
}
