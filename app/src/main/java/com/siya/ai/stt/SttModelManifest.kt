package com.siya.ai.stt

/** Immutable metadata for the Hindi ASR model expected by the runtime. */
data class SttModelManifest(
    val language: String = "hi",
    val modelFile: String = SttModelStore.MODEL_FILE,
    val tokensFile: String = SttModelStore.TOKENS_FILE,
    val sampleRate: Int = 16_000,
    val architecture: String = "indicconformer_ctc",
    val quantization: String = "int8",
) {
    init {
        require(language == "hi")
        require(sampleRate == 16_000)
        require(modelFile.endsWith(".onnx"))
        require(tokensFile == SttModelStore.TOKENS_FILE)
    }
}
