# Siya Ai

**Siya Ai** is being built as a privacy-first, on-device Android voice agent.

## Direction

- Android-first, mobile UI
- Voice-first interaction
- VAD -> STT -> LLM -> TTS pipeline
- Local model execution; no mandatory cloud AI server
- Foreground voice service with explicit microphone permission
- Dynamic model loading/unloading for memory efficiency
- Streaming responses and barge-in support planned

## Build roadmap

The project is implemented in 10 controlled parts. Current milestone: **Part 5 — Local LLM Brain**.

1. Android foundation
2. Audio engine
3. Silero VAD
4. Hindi STT
5. Local LLM brain ← **current**
6. Local TTS
7. Full-duplex conversation
8. Android agent actions
9. Background/resource manager
10. Security, performance and production hardening

## Local engine layer

1. Silero VAD (ONNX)
2. Sherpa-ONNX STT
3. Quantized Qwen GGUF through llama.cpp
4. Local TTS engine

### Part 5 status

The Android-side LLM boundary is now in place:

- `agent/llm/LlamaJni.kt` — native llama.cpp boundary
- `agent/llm/LlmConfig.kt` — mobile runtime configuration
- `agent/llm/PromptFormatter.kt` — Siya prompt/chat-template boundary
- `agent/llm/LocalLlmEngine.kt` — load/generate/cancel/unload lifecycle
- `agent/llm/LocalModelManager.kt` — local model path and state
- `docs/PART_05_LOCAL_LLM.md` — implementation and acceptance plan

The actual GGUF binary and native llama.cpp `.so` files are **not** committed to Git. They will be supplied through the model-pack/native build workflow. This keeps the repository lightweight and avoids pretending that the native runtime is already bundled.

## Model path

```text
<app filesDir>/models/llm/qwen2.5-1.5b-instruct-q4_k_m.gguf
```

## Project structure

```text
Siya-Ai/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/siya/ai/
│       │   ├── MainActivity.kt
│       │   ├── agent/llm/
│       │   ├── service/SiyaVoiceService.kt
│       │   └── ui/SiyaApp.kt
│       └── res/
├── docs/
│   └── PART_05_LOCAL_LLM.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

This repository is the clean starting point for Siya Ai. Older Aura/FFX project code is intentionally not used here.
