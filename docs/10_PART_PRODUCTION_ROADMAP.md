# Siya Ai — 10-Part Production Roadmap

## लक्ष्य
Siya Ai को चरणबद्ध तरीके से 100% on-device Android voice agent में बदलना। हर भाग अलग buildable milestone होगा। Internet/server dependency runtime में नहीं होगी।

## Part 1 — Android Foundation & App Shell
- Android project structure
- Siya Ai branding/package configuration
- Modern mobile-first UI
- Permission handling
- App lifecycle/state management
- Basic settings and diagnostics
- Build/debug baseline

**Output:** installable Siya Ai shell.

## Part 2 — Audio Engine & Microphone Pipeline
- AudioRecord 16 kHz PCM input
- Audio buffering/ring buffer
- Audio session configuration
- Input device handling
- Audio focus
- Echo/noise processing where supported by device
- Foreground microphone service foundation

**Output:** reliable local microphone stream.

## Part 3 — VAD / Wake & Speech Detection
- Silero VAD ONNX integration
- Voice start/stop detection
- Silence timeout
- Conversation state machine
- Lightweight idle mode
- Barge-in signal generation

**Output:** app only activates heavy processing when speech is detected.

## Part 4 — Offline Hindi STT
- Sherpa-ONNX runtime integration
- Hindi/multilingual Zipformer model packaging/import
- Streaming recognition
- Partial/final transcripts
- Audio-to-text pipeline
- STT lifecycle and memory release

**Output:** spoken Hindi → local text without internet.

## Part 5 — Offline LLM Brain
- llama.cpp Android/NDK integration
- GGUF model loading
- Qwen 1.5B-class quantized model support
- Prompt/session manager
- Token streaming
- Context/history limits
- Model load/unload lifecycle
- CPU/GPU/Vulkan capability detection

**Output:** local text → local reasoning/response.

## Part 6 — Offline TTS Voice
- Local ONNX TTS engine integration
- Hindi-capable voice/model selection
- Streaming/chunked synthesis
- AudioTrack playback
- Queue management
- Pause/stop controls
- Barge-in cancellation

**Output:** local response → spoken voice.

## Part 7 — Full Duplex Conversation Engine
- VAD → STT → LLM → TTS orchestration
- Streaming pipeline
- Sentence/phrase chunking
- Immediate interruption handling
- Conversation turn state
- Timeout and recovery
- End-to-end latency instrumentation

**Output:** natural phone-call-like local conversation.

## Part 8 — Android Agent Actions
- Intent/action router
- App launching
- Search/open actions
- Media controls where Android permits
- Notifications and basic device actions
- Safe confirmation for sensitive actions
- LLM bypass for deterministic commands

**Output:** Siya Ai can perform useful device tasks locally.

## Part 9 — Background, Resource & Battery Engineering
- Foreground service compliance
- Screen-off behavior testing
- Doze/battery behavior handling
- Dynamic model loading
- Idle/light/heavy memory tiers
- Model cache/mmap strategy
- Thermal throttling awareness
- Crash recovery/watchdog

**Important:** Android/OEM restrictions mean unrestricted guaranteed 24/7 microphone access cannot be promised on every device. The implementation must follow platform permissions and foreground-service rules.

**Output:** production-oriented background operation with graceful degradation.

## Part 10 — Production Hardening & Release
- Security/privacy audit
- No-network runtime verification
- Model integrity/checksum verification
- Offline first-run/model installation flow
- Storage management
- Error reporting without uploading private audio/text
- Performance benchmarks
- Low-RAM testing
- Battery/thermal testing
- Release build/signing configuration
- Documentation and final QA checklist

**Output:** production-ready Siya Ai release candidate.

## Dependency Order
1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10

## Build Rule
हर Part को पहले compile/run/test किया जाएगा। उसके बाद ही अगले Part में जाया जाएगा। बड़े native/model components को एक साथ डालकर build को unstable नहीं किया जाएगा।

## Model Budget Note
मॉडल size, RAM usage, startup time और latency device/model quantization पर निर्भर करेंगे। Blueprint में दिए गए numbers को targets माना जाएगा, guaranteed specifications नहीं।
