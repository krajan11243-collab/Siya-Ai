# Part 4 — Hindi STT

## Goal

Provide high-quality, offline Hindi speech-to-text on Android while keeping audio capture and native inference isolated from each other.

## Architecture

```text
AudioRecord 16 kHz PCM16
        |
        v
   Part 3 VAD
        |
        v
 speech segment buffer
        |
        v
   SttExecutor
        |
        v
 HindiSttEngine
        |
        v
 sherpa-onnx OfflineRecognizer
        |
        v
 IndicConformer CTC INT8
        |
        v
    SttResult
```

## Folder layout

```text
app/src/main/java/com/siya/ai/
├── audio/
├── service/
├── vad/
└── stt/
    ├── SttConfig.kt
    ├── SttResult.kt
    ├── SttModelStore.kt
    ├── HindiSttEngine.kt
    └── SttExecutor.kt
```

The STT code is intentionally separated from audio, VAD, service, and UI code. This keeps model changes and debugging manageable.

## Model contract

The app does **not** silently download an ASR model at runtime. The local model store expects:

```text
files/models/asr/hi/model.int8.onnx
files/models/asr/hi/tokens.txt
```

The current target is an INT8 IndicConformer CTC export compatible with sherpa-onnx. A suitable public mobile-oriented export is documented in the project notes and should be installed/imported before enabling Hindi STT.

## Runtime rules

- Input: mono PCM16 at 16 kHz.
- ASR runs on a dedicated single worker thread.
- AudioRecord thread never waits for native ASR inference.
- Native recognizer is serialized to avoid concurrent access and excessive RAM use.
- Model validation happens before native recognizer creation.
- No raw microphone samples are logged.
- `Result` is used for failure propagation rather than crashing the foreground service.
- The engine is explicitly released during service shutdown.

## Mobile strategy

Default CPU configuration uses 2 inference threads. This is deliberately conservative for 6–8 GB Android devices. A later hardware profile can choose thread count/provider based on device capabilities without changing the STT API.

The first release should keep the model outside the base APK when possible because ONNX model size can make APK installation unnecessarily large. A future model-import/download manager can verify SHA-256 before activation.

## Important accuracy note

Sherpa-ONNX officially supports local Android ASR and provides streaming Zipformer and other model families. For Hindi specifically, the current implementation uses an IndicConformer CTC model through the offline recognizer interface because a Hindi streaming Zipformer model is not the safest assumption. This gives a clean utterance-based boundary from Part 3 VAD and avoids inventing a nonexistent model.

## Verification status

Code-level tests cover configuration and result contracts. Full verification still requires:

1. Gradle dependency resolution.
2. Android APK build.
3. A valid Hindi model + tokens installed on the device.
4. Real microphone transcription tests.
5. Latency/RAM measurement on low, mid, and high-tier phones.

Do not mark Part 4 green until those runtime checks have been completed.
