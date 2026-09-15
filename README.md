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

## Planned engine layer

1. Silero VAD (ONNX)
2. Sherpa-ONNX STT
3. Quantized Qwen GGUF through llama.cpp
4. Local TTS engine

Model files will be kept separate from source code and added only through an explicit model-pack workflow.

## Project structure

```text
Siya-Ai/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/siya/ai/
│       │   ├── MainActivity.kt
│       │   ├── service/SiyaVoiceService.kt
│       │   └── ui/SiyaApp.kt
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

This repository is the new clean starting point for Siya Ai. Older Aura/FFX project code is intentionally not used here.
