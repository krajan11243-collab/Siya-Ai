# Part 2 — Audio Engine

## Scope
Part 2 establishes the real-time Android audio foundation only. VAD, STT, LLM and TTS are intentionally not started here.

## Input
- `AudioRecord`
- 16,000 Hz
- Mono
- PCM 16-bit
- `VOICE_RECOGNITION` preferred, `MIC` fallback
- Buffer is bounded and read on a dedicated thread
- Permission is checked before capture
- `ERROR_DEAD_OBJECT` / invalid operation stops the capture loop safely

## Output
- `AudioTrack` streaming mode
- 16,000 Hz mono PCM16
- Assistant speech attributes
- Explicit stop/flush/release lifecycle
- Ready for Part 6 TTS audio later

## Processing and safety
- `PcmRingBuffer` is thread-safe and bounded; overflow drops oldest samples.
- Only aggregate RMS/peak levels are calculated in Part 2.
- Raw microphone PCM is not logged or persisted.
- Optional `AcousticEchoCanceler` and `NoiseSuppressor` are attached only when supported by the device.
- Audio focus uses transient-may-duck; no `phoneCall` bypass is used.

## Service lifecycle
`SiyaVoiceService` checks `RECORD_AUDIO`, runs as a microphone foreground service, starts `AudioEngine`, updates the notification, and releases audio resources on destruction. If permission or audio initialization fails, it stops instead of pretending that capture is active.

## Verification
Automated unit tests cover the default 16 kHz/mono/PCM16 configuration and deterministic ring-buffer ordering/overflow/partial-read behavior.

A real microphone cannot be verified by JVM unit tests. Physical-device verification is still required for `AudioRecord`, device audio effects, audio focus behavior, and actual microphone capture. No Part 3 work should be considered complete until those checks pass on a real supported Android device.
