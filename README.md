# Gemma Heretic

A lightweight native Android GUI for local Ollama instances. Built for users who run models through Termux on Android or on LAN servers and want a fast, practical chat interface without the bloat.

## Features

- **Multi-endpoint support** — connect to Termux-local Ollama, LAN servers, or any reachable Ollama instance
- **Multi-model support** — query installed models, switch models per chat, track favorites
- **Streaming chat** — real-time token-by-token output with stop/regenerate/edit-and-resend
- **Markdown rendering** — inline formatting, headers, code blocks with copy button
- **Session history** — persistent chat storage with search, rename, delete
- **Per-chat configuration** — temperature, top_p, top_k, context size, max tokens, repeat penalty, seed, keep_alive, system prompt
- **Global defaults** — set baseline parameters that apply to new chats
- **Auto-title generation** — chats are automatically titled using the model
- **Dark/light theme** — terminal-inspired dark theme by default
- **Connection health checks** — test endpoints before use
- **Zero cloud dependencies** — everything runs locally, no accounts, no telemetry

## Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- An Ollama instance running and accessible

### Build

```bash
git clone <this-repo>
cd gemma-heretic
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or transfer the APK to your device and install manually.

## Connecting to Ollama

### Termux on the Same Device

1. Install Termux from F-Droid (not Play Store — the Play Store version is outdated)
2. Install Ollama: `pkg install ollama`
3. Start the server: `ollama serve`
4. Pull a model: `ollama pull gemma2:2b` (or any model)
5. In Gemma Heretic, add endpoint: `http://localhost:11434`

**Networking note:** On Android, each app runs in its own process, but `localhost` / `127.0.0.1` refers to the device's loopback interface, which is shared across all apps on the same device. This means the Gemma Heretic app can connect to an Ollama server running in Termux via `localhost:11434` without any special configuration. The app includes `android:networkSecurityConfig` with cleartext traffic permitted for these local HTTP connections.

If you encounter connection issues:
- Ensure Ollama is actively running in Termux (`ollama serve`)
- Ensure no firewall app is blocking local connections
- Try `http://127.0.0.1:11434` as an alternative

### LAN Server

1. On the server machine, set `OLLAMA_HOST=0.0.0.0` before starting Ollama
2. Start Ollama: `ollama serve`
3. Find the server's IP address (e.g., `192.168.1.100`)
4. In Gemma Heretic, add endpoint: `http://192.168.1.100:11434`
5. Ensure both devices are on the same network

### First Use

1. Open the app
2. Tap "Add Endpoint" on the welcome screen
3. Enter a name and URL for your Ollama server
4. Save and return to the home screen
5. Tap + to start a new chat
6. Select a model from the list
7. Start chatting

## Architecture

```
com.gemmaheretic.app/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs (Endpoint, ChatSession, ChatMessage, FavoriteModel)
│   │   ├── database/     # Room database definition
│   │   ├── entity/       # Room entities
│   │   └── preferences/  # DataStore preferences wrapper
│   └── repository/       # Repositories (Chat, Endpoint)
├── di/                   # Manual dependency injection container
├── domain/
│   └── model/            # Domain models and state types
├── network/
│   ├── api/              # Retrofit service + request/response models
│   └── streaming/        # Raw OkHttp streaming chat client
└── ui/
    ├── about/            # About screen
    ├── chat/             # Chat screen, settings panel, ViewModel
    ├── components/       # Shared components (MarkdownText)
    ├── endpoints/        # Endpoint management screen + ViewModel
    ├── history/          # Chat list, new chat sheet, ViewModel
    ├── models/           # Model browser screen + ViewModel
    ├── navigation/       # Navigation routes and host
    ├── settings/         # Settings screen + ViewModel
    └── theme/            # Material 3 color scheme and theme
```

### Key Design Decisions

- **Manual DI over Hilt/Dagger** — eliminates annotation processing overhead and keeps the dependency graph simple. The `AppContainer` creates all dependencies at app start with minimal allocation.
- **Retrofit for REST + raw OkHttp for streaming** — Retrofit handles simple REST calls (model listing, health checks, non-streaming chat). Streaming uses a raw OkHttp call with `BufferedReader` for line-by-line NDJSON parsing, avoiding SSE library overhead.
- **Room for persistence** — provides compile-time query validation, reactive Flow-based queries, and efficient SQLite access without manual cursor management.
- **DataStore for preferences** — lightweight key-value storage for user settings, avoiding SharedPreferences synchronization issues.
- **StateFlow over LiveData** — more idiomatic with Kotlin coroutines, better null safety, and more efficient for Compose consumption.
- **callbackFlow for streaming** — provides backpressure-aware, cancellation-safe streaming that integrates cleanly with the coroutine lifecycle.

## Performance Design

### How the App Stays Lightweight

- **Minimal dependency footprint** — only essential libraries: Compose, Room, DataStore, Retrofit, OkHttp, Gson. No Hilt, no Coil, no heavy third-party UI libraries.
- **No unnecessary recomposition** — state is scoped per-screen with `StateFlow`, collected with `collectAsState()`. UI state classes use stable data classes.
- **Lazy rendering** — `LazyColumn` for chat messages and lists. Long conversations don't load all messages into the composition tree at once.
- **Efficient streaming** — tokens are appended to a `StringBuilder` and the accumulated string is emitted to the UI. The streaming bubble is a single recomposing item at the bottom of the list.
- **Minimal background work** — no services, no periodic syncs, no background polling. The app only does network work when the user initiates it.
- **No animations** — deliberate omission of transition animations, shimmer effects, and animated indicators to minimize frame budget consumption.
- **Fast startup** — `AppContainer` initializes lazily where possible. Room database is only opened on first query. DataStore reads are cached after first access.
- **Edge-to-edge without overhead** — uses `enableEdgeToEdge()` for modern appearance without custom inset handling complexity.
- **Compact state models** — per-chat configuration is stored in the session entity, not in a separate preferences scope, avoiding cross-reference overhead.

### Android/Termux Networking Caveats

1. **Cleartext HTTP required** — Ollama serves over HTTP by default. The app includes a `network_security_config.xml` that permits cleartext traffic. This is necessary for local connections but means traffic to LAN endpoints is unencrypted.
2. **Localhost accessibility** — Android apps can reach `localhost:11434` when Ollama runs in Termux on the same device. Both apps share the device's network stack via the Linux loopback interface.
3. **Battery/Doze mode** — if the device enters Doze mode during a long generation, the network connection may be interrupted. The app handles this gracefully by saving whatever was streamed before the interruption.
4. **Large context windows** — running large context sizes (8k+) on-device in Termux will consume significant RAM. The app passes `num_ctx` to Ollama but does not attempt to manage Termux's memory.

## Configuration Reference

| Parameter | Default | Description |
|-----------|---------|-------------|
| Temperature | (model default) | Controls randomness. Lower = more deterministic. |
| Top P | (model default) | Nucleus sampling threshold. |
| Top K | (model default) | Limits token selection to top K candidates. |
| Context Size (num_ctx) | (model default) | Number of tokens in the context window. |
| Max Tokens (num_predict) | (model default) | Maximum tokens to generate. |
| Repeat Penalty | (model default) | Penalizes token repetition. |
| Seed | (model default) | Random seed for reproducibility. -1 = random. |
| Keep Alive | (model default) | How long to keep the model loaded. e.g., "5m", "1h". |
| System Prompt | (none) | Instructions prepended to every conversation. |
| Stream | true | Enable/disable streaming token output. |
| Auto Title | true | Auto-generate chat titles from first message. |

## Tradeoffs

- **No Hilt/Dagger** — simpler build, but DI is manual. Acceptable for this app's scope.
- **No offline model management** — the app queries models from Ollama but cannot pull/delete models. Use the Ollama CLI for model management.
- **No image/multimodal support** — text-only in v1. Multimodal would add significant complexity.
- **Cleartext HTTP only** — no HTTPS support for Ollama endpoints. Acceptable for local/LAN use.
- **Single-activity architecture** — all screens are Compose destinations in one Activity. Works well for this app's complexity level.
- **No markdown tables** — the markdown parser handles bold, italic, inline code, code blocks, and headers. Tables and other complex markdown are rendered as plain text.

## Future Roadmap

- [ ] Pull/delete models from within the app
- [ ] Export/import chat history
- [ ] Markdown table rendering
- [ ] Syntax highlighting in code blocks
- [ ] Chat folders/tags for organization
- [ ] Swipe-to-delete on chat list
- [ ] System prompt templates/presets
- [ ] Response time statistics dashboard
- [ ] Widget for quick new chat
- [ ] mDNS/Bonjour discovery of LAN Ollama instances
- [ ] HTTPS/TLS support for remote endpoints
- [ ] Multimodal support (images) when Ollama supports it well
- [ ] Tablet/landscape layout optimization
- [ ] Share text to Gemma Heretic from other apps

## Tech Stack

| Component | Library | Version |
|-----------|---------|---------|
| Language | Kotlin | 1.9.22 |
| UI | Jetpack Compose | BOM 2024.01.00 |
| Design | Material 3 | (via Compose BOM) |
| Database | Room | 2.6.1 |
| Preferences | DataStore | 1.0.0 |
| Networking | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| JSON | Gson | 2.10.1 |
| Navigation | Compose Navigation | 2.7.6 |
| Build | AGP | 8.2.2 |
| Min SDK | 26 (Android 8.0) | |
| Target SDK | 34 (Android 14) | |

## License

MIT
