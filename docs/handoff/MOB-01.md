# Handoff — setup session -> MOB-01 (29-06-2026)

This bootstrap session built the app v0.1->v0.7 plus the launcher / context-guard /
handoff infrastructure, and grew large; the owner asked to start the MOB session
series fresh. Handing the project to MOB-01.

## State now
The app "CitesteMi" is at **v0.7**, shipped on GitHub Releases
(github.com/crxh22/clipboard-reader, PUBLIC). `main` is fully committed + pushed.
The new minimal-white + player UI shipped in v0.7 was **built blind** — Android
layouts cannot be rendered on this server, so its real on-phone appearance is
**unconfirmed**; the owner is reviewing it now. Build is green; 11 JVM unit tests
pass. The debug APK signature is identical across every build (`ce735aaa...`), so each
version installs over the previous one with no uninstall. Code lives under
`app/src/main/java/com/clipboardreader/{reader,intake,tile,bubble}`; docs are
README.md / SPEC.md / PLAN.md / docs/.

## Done this session
Built the whole app v0.1 -> v0.7 and the project infrastructure (detail in git
history + GitHub Releases v0.1..v0.7):
- TTS reader: on-device `TextToSpeech`, auto ro/ru by Cyrillic script
  (`reader/LanguageDetector.kt`), position-aware engine with pause/resume + word-skip
  (`reader/SpeechEngine.kt` + pure `reader/Playback.kt`, unit-tested).
- Intake: PROCESS_TEXT ("Citeste cu voce"), Share, Quick-Settings tile, floating
  bubble, in-app button (`intake/`, `tile/`, `bubble/`).
- Bubble: tap = play/pause/read, double-tap = open app, long-press = ⏪/⏩ (skip 2
  words; hide-timer resets per press), drag = move + drag-onto-X to close.
- UI: minimal-white + Spotify-style player controls, blue accent, modern Material
  switch at the top (no scroll), compact now-reading card.
- Infra: local Android build env, GitHub repo + releases, `clipboard_canon.sh`
  launcher, `.claude` context-guard hook, and the handoff runbook/template/checker
  (`docs/runbooks/`, `scripts/check_handoff.py`).

## In progress
NONE — clean boundary. v0.7 is shipped and all infrastructure is committed. The only
open loop is awaiting the owner's feedback on v0.7.

## Open decisions
- **v0.7 UI visual confirmation** (owner decides): the screen was built without being
  able to see the rendered Android UI. If he reports visual issues, edit
  `app/src/main/res/layout/activity_main.xml` + `res/values/themes.xml` (+ drawables).
  Trigger: his next message about how v0.7 looks.
- **App icon + final name** (owner + you): still the placeholder system mic icon;
  deferred until the function is solid.
- **CI on GitHub Actions** (owner): workflow YAML exists only as a scratchpad `.bak`
  because the `gh` token lacks `workflow` scope; needs
  `gh auth refresh -h github.com -s workflow` (an owner action). Until then build +
  release locally.
- **Stable release signing key**: currently debug-signed (fine for sideload, consistent
  across local builds). For CI-built or Play-Store releases, generate a keystore. Deferred.

## Gotchas / constraints
- ALWAYS `source ~/.clipboard-reader-env` before any gradle/sdkmanager command (it
  sets JAVA_HOME + ANDROID_HOME). Build: `./gradlew assembleDebug`.
- GitHub: the ssh key on this box is a deploy key scoped to **SF-F5 only** — for THIS
  repo use HTTPS + the `gh` token (already set via `gh auth setup-git`). Commits MUST
  use the noreply email `214634318+crxh22@users.noreply.github.com` (gmail is blocked
  by GitHub). The `gh` token lacks `workflow` scope (cannot push `.github/workflows/*`).
- **Android UI cannot be rendered on this server** (no device/emulator) → ship the APK
  and let the owner review; only build + unit tests are machine-verifiable. Regression
  lesson: a `GestureDetector`-based bubble swallowed taps (v0.4); fixed with explicit
  ACTION_DOWN/MOVE/UP handling (v0.5) — do NOT reintroduce GestureDetector for the bubble.
- Clipboard read works only with window focus: `intake/ClipboardReadActivity.kt` reads
  in `onWindowFocusChanged(true)`, NOT onResume (onResume runs before focus → empty).

## Next action
Wait for the owner's feedback on v0.7 (he is testing on his phone). When he replies:
if he reports UI tweaks, edit `app/src/main/res/layout/activity_main.xml`
(+ themes/drawables), bump `versionCode`/`versionName` in `app/build.gradle.kts`,
`source ~/.clipboard-reader-env && ./gradlew assembleDebug`, then
`gh release create vX.Y --repo crxh22/clipboard-reader --title "CitesteMi vX.Y" --notes-file <notes> CitesteMi-vX.Y.apk`
and send him the direct download link. If he confirms v0.7 is good, the next planned
polish is a real app icon + a stable signing key. Observable success = he installs the
new APK over the old one and confirms it looks/works right.

## Verification status
- **Machine-verified:** `./gradlew assembleDebug` + `testDebugUnitTest` pass (11 unit
  tests — LanguageDetector 5, Playback 6); APK signature consistent (`ce735aaa...`);
  release download URLs return the APK (HTTP 200, vnd.android.package-archive).
- **ASSUMED — owner's phone only (not verifiable here):** TTS audio + ro/ru voice
  quality; the floating-bubble gestures (tap / double-tap / long-press / drag-to-X);
  the Quick-Settings tile; the real on-screen appearance of the v0.7 UI. Treat these
  as unverified until the owner confirms.
