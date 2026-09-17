# Part 5 — Local LLM Brain

## Goal

Connect Hindi STT output to a fully on-device Qwen instruct model through llama.cpp. No API key or cloud inference is required for inference after the model is installed.

## Model

- Model: Qwen2.5 1.5B Instruct
- Quantization: Q4_K_M
- Format: GGUF
- Source: `Qwen/Qwen2.5-1.5B-Instruct-GGUF`
- File: `qwen2.5-1.5b-instruct-q4_k_m.gguf`
- Expected size: about 1.12 GB
- SHA-256 is pinned in `LlmModelStore` and verified before installation.

The large GGUF is intentionally not committed to Git.

## Runtime

The Android app uses `dev.ffmpegkit-maintained:llama-android:0.1.1`, a third-party prebuilt llama.cpp Android AAR. The runtime is CPU-only in this integration (`gpuLayers = 0`). GPU acceleration is not claimed until separately verified on target hardware.

## Files

- `LlmConfig.kt` — mobile-adaptive context/thread/generation settings.
- `LlmModelStore.kt` — model location and integrity contract, including shared backup/restore support.
- `LlmModelInstaller.kt` — resumable download, temporary file, size check, SHA-256 check, atomic install, and shared backup.
- `LlmFastPath.kt` — immediate answers for trivial greetings without invoking the 1.12 GB model.
- `LocalLlmEngine.kt` — load/complete/release facade over llama.cpp.
- `LlmExecutor.kt` — serializes inference and applies the greeting fast path.

## Pipeline

`AudioRecord → Silero VAD → complete speech segment → Hindi Sherpa-ONNX STT → Qwen2.5 local LLM`

The voice service forwards successful Hindi STT text to `LlmExecutor`.

## Model persistence

The working copy is kept in app storage for llama.cpp. A user-visible backup is also maintained under `Download/Siya Ai/Models/LLM/` through Android MediaStore on supported Android versions. This allows the model to be restored after app-data loss/reinstall without unrestricted filesystem access. The app does not require `MANAGE_EXTERNAL_STORAGE` for this feature.

## Network boundary

`INTERNET` is required only for first-time model download/import workflows. Once the GGUF is present locally, llama.cpp inference itself does not require network access.

## Low-latency defaults

Current defaults are intentionally conservative for short voice turns:

- context: 512 tokens
- CPU threads: up to 6, based on available processors
- max output: 12 tokens
- temperature: 0.25
- top-p: 0.9
- top-k: 40
- GPU layers: 0

A warm-up path loads the model before the first voice turn when the model is installed. Trivial greetings use the fast path and do not wait for GGUF generation. Actual generation latency remains device-dependent and is not guaranteed to be a fixed number of seconds.

## Verification gate

Part 5 is considered build-verified only when all repository tests pass and the debug APK packages/signs successfully. Real-device GGUF inference still requires a physical-device test with the actual model file; CI cannot prove that because the ~1.12 GB model is not stored in the repository.

Required real-device checks before Part 6:

1. Install/import the Qwen GGUF and confirm SHA-256 verification.
2. Load the model successfully on the target Android phone.
3. Send a normal prompt and receive a local response.
4. Confirm Hindi STT text reaches the LLM path.
5. Confirm a simple greeting uses the fast path.
6. Stop/restart the voice service and confirm model lifecycle remains stable.
7. Confirm the model is reused after an app update and can be restored from the shared backup after app-data loss.

Only after these checks should Part 6 begin.
