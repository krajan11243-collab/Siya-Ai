package com.siya.ai.vad

import android.content.Context
import com.siya.ai.storage.SharedModelBackup
import java.io.File

class VadModelStore(context: Context) {
    companion object { const val FILE_NAME = "silero_vad_v5.onnx" }

    private val legacyDir = File(context.filesDir, "models/vad")
    private val externalDir = File(
        context.getExternalFilesDir(null) ?: context.filesDir,
        "SiyaAi/Models/VAD"
    )
    private val modelDir = if (File(legacyDir, FILE_NAME).isFile) legacyDir else externalDir
        .apply { mkdirs() }
    private val sharedBackup = SharedModelBackup(context)

    val modelFile: File get() = File(modelDir, FILE_NAME)

    fun isInstalled(): Boolean = modelFile.isFile && modelFile.length() > 100_000L
    fun readBytes(): ByteArray = require(isInstalled()) { "Silero VAD model is not installed" }.let { modelFile.readBytes() }
    fun backupToShared(): Boolean = sharedBackup.copyToShared(modelFile, SharedModelBackup.VAD_PATH)
    fun restoreFromShared(): Boolean = sharedBackup.restoreTo(FILE_NAME, SharedModelBackup.VAD_PATH, modelFile, 100_000L)
}
