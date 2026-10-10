# readmeclub-android11

readme.club's Android app for e-ink readers, built on the Xteink S4 (Android 11, 4.3" e-ink) and meant to run on any Android 11+ reader. Overview: README.md; device specifics: docs/DEVICE.md.

## Non-negotiable rules
- minSdk 30. No dependency on Google Play Services.
- No animation, no colour carrying meaning, no continuous scrolling: everything is paginated.
- One bottom bar on every screen, the same everywhere (48 dp, divider above, `PagerButton` style; the tab bar stays under it on the main screens). Never assume a hardware button: every action is reachable on screen.
  - **Back** is always bottom left, always labelled "Back", and shown whenever there is somewhere to go back to; never a back control at the top.
  - The **right** button is the screen's main action: Next on a paginated screen, Menu in the reader, Save on a wallpaper.
  - **Previous** sits just before the page number ("2 / 5"); with no Back (main tabs) it is the leftmost button. Next never wraps around.
  - Page keys (volume, page up/down, the S4's button) turn pages wherever there are pages.
- Small screens first: designed for the S4 (350 × 584 dp), then adapted. Touch targets ≥ 44 dp (choices 52 dp), body text ≥ 15 sp, labels ≥ 11 sp, at most 4 choices per screen, text that does not fit is paginated. Larger screens add columns and margins, never smaller targets.
- Design system (www.readme.club/brand, in black and white): new screens use the `Ds.*` styles and `ds_*` drawables (`res/values/ds_*.xml`), never raw colours or sizes. Every component shows on the diagnostic's Components screen. Fonts: Space Grotesk Bold for titles, Space Mono Bold for numbers (Latin subsets, OFL, see `licenses/`); body text uses the system sans.
- APK budget: 450 KB. CI fails a build above it.
- The UI only reads the local cache; only the `sync` package writes from the network.
- No dependency-injection framework: the App class is the container.
- Every new dependency must be justified in the PR.
- No link leaves the app: the S4 has no browser. Only links to readme.club articles and guides stay active, and they open in the app.
- Everything user-facing and everything in the repo is written in English.
- **Every change is recorded in CHANGELOG.md, in the same commit**: user-visible changes and changes to the build or release process, under the upcoming version's section (create it, dated "Unreleased", when bumping `versionName`). Set the date when the version is tagged. The release job refuses a tag without its section and uses it as the release notes.

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
