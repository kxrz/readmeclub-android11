# readmeclub-android11

readme.club's Android app for e-ink readers, built on the Xteink S4 (Android 11, 4.3" e-ink) and meant to run on any Android 11+ reader. Overview: README.md; device specifics: docs/DEVICE.md.

## Non-negotiable rules
- minSdk 30. No dependency on Google Play Services.
- No animation, no colour carrying meaning, no continuous scrolling: everything is paginated.
- The UI only reads the local cache; only the `sync` package writes from the network.
- No dependency-injection framework: the App class is the container.
- Every new dependency must be justified in the PR.
- No link leaves the app: the S4 has no browser. Only links to readme.club articles and guides stay active, and they open in the app.
- Everything user-facing and everything in the repo is written in English.

## Commands
- Build: ./gradlew assembleDebug
- Tests: ./gradlew testDebugUnitTest
- Install on the S4: adb install -r app/build/outputs/apk/debug/app-debug.apk

## Verification
- The `reader` engine has unit tests (pagination of known layouts, edge cases: image taller than the screen, paragraph over 3 pages).
- Before calling something done: green build (debug and release) and green tests on GitHub Actions, then a test on the device.

## Build
- The cloud environment has no Android SDK nor access to Google Maven: builds are verified on GitHub Actions (`.github/workflows/android.yml`).
- Every push produces the `app-debug` artifact (APK signed with `app/debug.keystore`, shared so successive installs go through).
- A `v*` tag produces the signed release APK and publishes it on GitHub Releases: full procedure in docs/RELEASE.md.
- Debug builds have their own id (`club.readme.android.debug`, "readme.club dev") and install next to the release.
- Deliberately minimal stack: native Android views, Kotlin, no AndroidX; networking with `HttpURLConnection` + `org.json`.
