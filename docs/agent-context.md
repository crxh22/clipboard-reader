# Agent context — clipboard-reader (CitesteMi)

Operational knowledge for any Claude session working on this app. The app's *what*
is in README.md / SPEC.md / PLAN.md; this file is the *how* — build, release, and
how to talk to the owner. (Injected into every session by `clipboard_canon.sh`.)

## What this is
CitesteMi: an Android app (Kotlin + Gradle) that reads selected / clipboard / shared
text aloud via on-device TTS. Romanian + Russian, auto-detected by script (Cyrillic → ru).

## Build & verify (on this server)
- Env is already installed: JDK 17, Android SDK at `~/android-sdk`, Gradle wrapper in repo.
  Before any gradle/sdkmanager command: `source ~/.clipboard-reader-env`.
- Build: `cd /home/artur/projects/clipboard-reader && source ~/.clipboard-reader-env && ./gradlew assembleDebug`
  → `app/build/outputs/apk/debug/app-debug.apk`.
- Unit tests: `./gradlew testDebugUnitTest`. Never claim a build/test passes without running it.
- `local.properties` (gitignored) holds `sdk.dir`.

## Release to the owner's phone
- He installs by downloading a GitHub Release APK on his phone (public repo, no login).
- Per version: bump `versionCode` + `versionName` in `app/build.gradle.kts`, then
  `cp app-debug.apk CitesteMi-vX.Y.apk && gh release create vX.Y --repo crxh22/clipboard-reader --title "CitesteMi vX.Y" --notes-file <notes> CitesteMi-vX.Y.apk`.
- All local builds share one debug keystore → same signature → he installs over the old
  version without uninstalling (verify with `apksigner verify --print-certs` if unsure).

## GitHub (differs from SF-F5 — important)
- Repo **crxh22/clipboard-reader (PUBLIC)**. The ssh key here is a deploy key scoped to
  SF-F5 ONLY → for this repo use HTTPS + the `gh` token (`gh auth setup-git`, https remote).
- Commits MUST use the noreply email `214634318+crxh22@users.noreply.github.com`
  (GitHub blocks the gmail). Set it repo-local.
- The `gh` token LACKS `workflow` scope → cannot push `.github/workflows/*`. CI is deferred
  until `gh auth refresh -h github.com -s workflow` (an owner action).

## Talking to the owner (Artur)
- Romanian, plain language, **no diacritics**. Non-technical, phone-primary.
- After each shipped version: report concisely and ALWAYS include the direct APK link.
- Be honest: runtime behaviour (voice, overlay bubble, tile) is only verifiable on his
  phone — say so; build + unit tests are all that's machine-verified.
- Surface real decisions with a recommendation; no blind technical questions.

## Android design constraint (why it works this way)
Background clipboard reading is blocked since Android 10, so reading is triggered by:
PROCESS_TEXT ("Citeste cu voce" in the text-selection menu — Chrome), Share, a Quick
Settings tile, a floating bubble (the Facebook one-tap path), and an in-app button.

## Where state lives
Shipped versions + notes: GitHub Releases. Current plan + open items: PLAN.md. The
chosen UI direction + design mockups: docs/design/. No other hidden state.

## Code conventions
Kotlin; English code/comments/commits; small verifiable changes; keep the existing
`app/src/main/java/com/clipboardreader/{reader,intake,tile,bubble}` layout. Implement
what SPEC/PLAN ask — no speculative gold-plating.
