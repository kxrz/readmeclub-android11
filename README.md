# readme.club for Android e-ink

A lightweight, offline-first Android app that brings [readme.club](https://www.readme.club)
to e-ink readers, starting with the **Xteink S4** (Android 11, 4.3" screen).

- **News** — the latest articles, cached for offline reading.
- **Guides** — a bookshelf of brands, each with its device guides, all readable offline.
- **Wallpapers** — the gallery filtered to images that fit the screen, saved to
  `Pictures/ReadmeClub` in one tap.

Everything is paginated (no scrolling), black on white, without animations, and readable
with the device's single capacitive button.

## Install

Download `readmeclub.apk` from the [latest release](../../releases/latest) and sideload it
(allow installing from unknown sources), or from a computer:

```sh
adb install -r readmeclub.apk
```

## Build

```sh
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds install as a separate app, *readme.club dev*. GitHub Actions builds every push;
a `v*` tag publishes a signed release (see [docs/RELEASE.md](docs/RELEASE.md)).

## How it works

- Pure Kotlin and native Android views — no AndroidX, no third-party libraries.
- Content comes from readme.club's public APIs; the UI only ever reads the local cache,
  filled by the `sync` package.
- The reader renders article HTML with `Html.fromHtml` and splits it into screen-sized
  pages (`reader/Paginator.kt`).
- Device specifics (button keycode, refresh trick, screen size): [docs/DEVICE.md](docs/DEVICE.md).

## Contributing

Issues and pull requests are welcome. Keep the rules in [CLAUDE.md](CLAUDE.md): no new
dependency without a reason, no animation, no scrolling, no link leaving the app.

## License

[GPL-3.0](LICENSE), with additional terms in [NOTICE](NOTICE): any distributed version
must keep the attribution *“Based on readme.club for Android by Florent Bertiaux”*, and
the readme.club name, logo and icon are not licensed for use by modified versions.

readme.club is independent and not affiliated with Xteink or any device brand.
