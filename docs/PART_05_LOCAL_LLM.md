# Siya Ai — Part 5: Local LLM Brain

## Goal

Add the local reasoning layer for Siya Ai without making the Android UI or voice service depend directly on native inference code.

## Current implementation

- `LlamaJni.kt`: isolated JNI boundary for the native `siya_llama` library.
- `LlmConfig.kt`: mobile-safe runtime configuration (context, threads, max tokens, temperature).
- `PromptFormatter.kt`: Siya system prompt and chat-template boundary.
- `LocalLlmEngine.kt`: lifecycle-safe facade for load → generate → cancel → unload.
- `LocalModelManager.kt`: private model directory and Qwen GGUF path management.

## Model

Target model: a compatible Qwen 2.5 1.5B Instruct GGUF quantization, initially Q4_K_M as specified by the master plan.

Large model binaries are intentionally **not committed to Git**. They should be installed through a model-pack/import workflow.

Expected local path:

```text
<app filesDir>/models/llm/qwen2.5-1.5b-instruct-q4_k_m.gguf
```

## Native engine

The JNI boundary expects a native library named:

```text
libsiya_llama.so
```

That library will wrap llama.cpp and expose four operations:

1. create(modelPath, contextSize, threads)
2. generate(handle, prompt, maxTokens, temperature)
3. cancel(handle)
4. destroy(handle)

The Android layer currently treats the native library as optional, so the project can still compile before the llama.cpp source and ABI builds are integrated.

## Lifecycle

```text
MODEL MISSING
     ↓ model imported
READY
     ↓ load()
LOADED
     ↓ generate()
GENERATING
     ↓ complete
LOADED
     ↓ unload()
READY
```

`cancel()` must stop generation before TTS barge-in handling proceeds. `unload()` releases the native handle and should be called when the resource manager decides the heavy model is no longer needed.

## Memory policy

The master plan's RAM numbers are targets, not guarantees. Actual RSS depends on quantization, context size, allocator, native backend, CPU/GPU offload and device RAM.

Initial mobile defaults:

- context: 2048 tokens
- threads: 4
- max output: 256 tokens
- temperature: 0.7

These are intentionally conservative and will be benchmarked on real hardware.

## Streaming follow-up

The current JNI contract returns a complete string so Part 5 can establish a stable engine boundary first. Part 7 will extend this contract with token callbacks/streaming so the first response chunks can reach the TTS layer without waiting for the full answer.

## Acceptance tests

- App remains buildable when native engine is absent.
- Missing model produces a controlled `ModelMissing` state.
- Invalid native handle never reaches generation.
- `cancel()` is safe before/after generation.
- `unload()` is idempotent.
- No model content is written to logs.
- Airplane mode does not affect the local LLM path after the model is installed.

## Next step

Part 5's next implementation step is the actual llama.cpp Android/NDK bridge and ABI builds. After that, integrate the engine into the voice service only after native load/generate/unload tests pass.
