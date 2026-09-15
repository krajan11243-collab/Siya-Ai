package com.siya.ai.stt

/**
 * Pinned model family contract used for the first Hindi offline release.
 * The files are downloaded/imported once; inference itself is fully offline.
 */
object SttModelSource {
    const val REPOSITORY = "parismitaglobalsolutions/indicconformer-sherpa-onnx"
    const val MODEL_URL =
        "https://huggingface.co/parismitaglobalsolutions/indicconformer-sherpa-onnx/resolve/main/hi/model.int8.onnx"
    const val TOKENS_URL =
        "https://huggingface.co/parismitaglobalsolutions/indicconformer-sherpa-onnx/resolve/main/tokens.txt"

    const val MODEL_ARCHITECTURE = "AI4Bharat IndicConformer CTC"
    const val QUANTIZATION = "INT8 MatMul"
    const val SAMPLE_RATE = 16_000
}
