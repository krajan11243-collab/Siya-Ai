# Part 5 — Local LLM Brain

## Goal

Connect Hindi STT output to a fully on-device Qwen instruct model through llama.cpp. No API key or cloud inference is required after the model is installed.

## Model

- Model: Qwen2.5 1.5B Instruct
- Quantization: Q4_K_M
- Format: GGUF
- Source: `Qwen/Qwen2.5-1.5B-Instruct-GGUF`
- File: `qwen2.5-1.5b-instruct-q4_k_m.gguf`
- Expected size: about 1.12 GB
- SHA-256 is pinned in `LlmModelStore`

The large GGUF is intentionally not committed to Git.

## Runtime

The Android app uses `dev.ffmpegkit-maintained:llama-android:0.1.1`, a prebuilt llama.cpp Android AAR. The current configuration uses CPU/NEON with `gpuLayers = 0`; GPU acceleration is not claimed until it is separately verified on target hardware.

## Files

- `LlmConfig.kt` — conservative mobile context/thread/generation settings.
- `LlmModelStore.kt` — app-private model location and integrity contract.
- `LlmModelInstaller.kt` — first-install download, temporary file, size check, SHA-256 check, atomic rename.
- `LocalLlmEngine.kt` — load/complete/release facade over llama.cpp.
- `LlmExecutor.kt` — serializes local inference because one model session is not thread-safe.

## Pipeline

`AudioRecord → Silero VAD → complete speech segment → Hindi Sherpa-ONNX STT → Qwen2.5 local LLM`

The voice service now forwards successful Hindi STT text to `LlmExecutor`.

## Network boundary

`INTERNET` is present only because the first-time model installer downloads the GGUF. Once the model is installed, llama.cpp loads it from the app-private filesystem and inference does not require network access.

## Memory policy

Defaults:

- context: 2048 tokens
- CPU threads: 4
- max output: 256 tokens
- temperature: 0.7
- top-p: 0.9
- GPU layers: 0

The Q4_K_M file is about 1.12 GB, but runtime RAM usage is device-dependent. These values must be benchmarked on real phones before production tuning.

## Verification status

**Code integration: implemented.**

**Full verification: pending.** The repository has not yet been successfully built and a real Android device has not yet loaded the GGUF and generated a response. Therefore Part 5 is not marked production-verified.

## Next gate

Before Part 6, build the complete app and test:

1. APK/Gradle build.
2. Model installation and SHA-256 verification.
3. Qwen model load.
4. Hindi STT text → Qwen response.
5. Model release without crash.

Only after these pass should Part 6 begin.
