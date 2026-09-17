# Siya Ai

**Siya Ai** is being built as a privacy-first, on-device Android voice agent.

## Direction

- Android-first, mobile UI
- Voice-first interaction
- VAD -> STT -> LLM -> TTS pipeline
- Local model execution; no mandatory cloud AI server
- Foreground voice service with explicit microphone permission
- Dynamic model loading/unloading for memory efficiency
- Streaming audio output and coordinated barge-in cancellation

## Build roadmap

The project is implemented in 10 controlled parts. Current work: **Part 6 — Local TTS + Part 7 — Full Duplex**.

1. Android foundation
2. Audio engine
3. VAD
4. Hindi STT
5. Local LLM brain
6. Local TTS
7. Full duplex conversation
8. Android agent actions
9. Background/resource manager
10. Security/production hardening

## Part 6/7 status

- Local TTS facade with Sherpa-ONNX Kokoro adapter and Android local-TTS fallback
- Cancellable Float-PCM -> AudioTrack output queue
- Sentence/phrase chunking and producer cancellation
- Duplex state phases: listening, thinking, speaking and interrupting
- LLM turn-generation guard and barge-in invalidation
- Per-turn timeout and recovery path

The neural Kokoro model is intentionally not committed to Git because model binaries are large. The app checks for an installed local model directory and reports the model/version status.

[model-hub] CI marker: model-hub rewrite workflows are legacy and must not modify current UI files.
