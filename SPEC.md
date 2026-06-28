# SPEC — Clipboard Reader (Android TTS)

Technical specification. Working title: **CitesteMi**. Personal-use Android
utility that speaks text aloud from the clipboard or from shared/selected text.

---

## 1. Goal & scope

Speak arbitrary text aloud using on-device Text-To-Speech, with simple playback
controls. Single user, personal device, sideloaded. No backend, no account, no
network dependency for the core feature, no analytics.

Non-goals (v1): cloud/neural voices, Play Store release, multi-platform (iOS),
document/PDF import.

---

## 2. Platform & stack

- **Android only.** `minSdk = 26` (Android 8.0 Oreo — covers ~98% of devices and
  enables Quick Settings tiles, foreground services, notification actions),
  `targetSdk = 34` (or latest stable at build time).
- **Language:** Kotlin.
- **UI:** Jetpack Compose (small surface, modern, low boilerplate). Plain Views
  acceptable if simpler for the chosen builder.
- **Build:** Gradle (Kotlin DSL) with the Gradle wrapper committed.
- **Dependencies:** keep minimal — AndroidX core, Compose, Lifecycle, a
  MediaSession for the player notification. No third-party TTS lib.

---

## 3. The clipboard constraint (design crux)

Since **Android 10 (API 29)** an app may read `ClipboardManager.getPrimaryClip()`
**only while it holds window focus (is in the foreground) or is the active IME**.
Background clipboard reads return null/stale. Therefore a hands-free background
"read whatever I copy" service is **not possible** for a normal app.

Supported trigger mechanisms (all reliable on modern Android):

| Trigger | Mechanism | Clipboard needed? | Notes |
|---|---|---|---|
| **Share** | `ACTION_SEND` (`text/plain`) intent filter | No — text arrives in the intent | Works from any app's share sheet. Most reliable. |
| **Select text -> Read aloud** | `ACTION_PROCESS_TEXT` intent filter | No — selected text in the intent | Adds an item to the text-selection popup menu. Smoothest UX. |
| **Quick Settings tile** | `TileService` -> launches a transparent, focusable Activity that reads the clipboard then `finish()`es | Yes — but the launched Activity HAS focus, so it is allowed | The "one tap from anywhere reads clipboard" experience the user asked for. |
| **Open the app** | Main Activity reads clipboard on resume | Yes — app is foreground | Fallback / manual path. |
| **Home-screen shortcut** | Static/dynamic shortcut -> the read-clipboard Activity | Yes, via focused Activity | Optional convenience. |

Design decision: **support Share + Process-Text + Quick-Settings-Tile + in-app**.
This gives the clipboard experience (tile/app) and the more-reliable select/share
path, with no single point of failure.

---

## 4. TTS

- Engine: `android.speech.tts.TextToSpeech` (default = Google TTS). Free, offline
  once voice data is installed.
- Default language **`ro-RO`** (Romanian); fall back to device default locale if
  Romanian voice data is unavailable.
- Handle `LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED`: prompt the user to install
  voice data (`ACTION_INSTALL_TTS_DATA`) or pick another installed voice.
- Per-utterance text limit (`getMaxSpeechInputLength()`, ~4000 chars): split long
  text into chunks (by sentence/paragraph) and queue them (`QUEUE_ADD`).
- Controls: rate (speed) and pitch, persisted in settings; Romanian default rate.
- `UtteranceProgressListener` drives progress, chunk advance, and notification state.

---

## 5. Architecture / components

- **`ReaderService`** — a foreground `Service` owning the `TextToSpeech` instance,
  the chunk queue, and a `MediaSession`. Shows a media-style notification with
  Play/Pause/Stop (and Skip in v2). Survives screen-off; controllable from
  lockscreen. Requests audio focus; pauses on transient loss (calls, other media).
- **`MainActivity`** — Compose UI: shows the current text, Play/Pause/Stop, speed
  slider, settings entry. On resume, offers/auto-reads the clipboard.
- **`ClipboardReadActivity`** — transparent, no-UI Activity launched by the tile /
  shortcut: reads clipboard, hands text to `ReaderService`, finishes immediately.
- **`ReadTileService`** — `TileService` for the Quick Settings tile.
- **Intent filters** — `ACTION_SEND` and `ACTION_PROCESS_TEXT` route incoming text
  to `ReaderService`.
- **Text chunker** — pure Kotlin util (sentence/paragraph split + char-limit
  packing); unit-testable without a device.

---

## 6. Permissions (minimal)

- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (API 34+).
- `POST_NOTIFICATIONS` (API 33+) — runtime prompt for the player notification.
- **No** clipboard permission exists or is needed (governed by focus, not a perm).
- **No** `INTERNET` for the core feature.
- Optional later: `SYSTEM_ALERT_WINDOW` (floating bubble, v2+).

---

## 7. Build & distribution

- No local Android toolchain on the dev server — **build in CI**.
- **GitHub Actions**: on every push, set up JDK 17 + Android SDK, run
  `./gradlew assembleDebug` (and `assembleRelease` once signing is set up), and
  upload the APK as a workflow artifact + attach to a GitHub Release on tags.
- v1 ships **debug-signed** (fine for personal sideload). For stable updates later,
  generate a keystore and store it in GitHub Actions secrets for signed release
  builds (consistent signature = in-place updates).
- Install path: download the APK from GitHub on the phone, allow "install unknown
  apps" for the browser once, tap to install. No Play Store.

---

## 8. Testing / verification

- **Unit tests** (JVM, in CI): text chunker, clipboard-text extraction, settings.
- **CI** is the compile/lint gate (no device in CI for v1).
- **Real verification** = manual on the founder's phone each phase (TTS, tile,
  share, lockscreen controls need a real device + voice). Instrumented/UI tests
  are optional, added if regressions appear.

---

## 9. Open decisions (see PLAN.md §"Decizii")

1. Trigger set — confirm the Quick-Settings tile is an acceptable "clipboard" UX.
2. Languages to read — Romanian-only first, or multi-language/auto-detect early.
3. App display name + icon (cosmetic, can be deferred).
