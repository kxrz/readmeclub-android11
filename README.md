<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/logo-dark.png">
  <img src="docs/assets/logo-light.png" alt="readme.club" width="260">
</picture>

### Everything your e-ink device needs, in one place — now on the device itself.

News, guides and wallpapers from [readme.club](https://www.readme.club),
in a tiny offline-first app built for e-ink screens.

[![Latest release](https://img.shields.io/github/v/release/kxrz/readmeclub-android11?style=flat-square&label=download&color=000000)](../../releases/latest)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-000000?style=flat-square)](LICENSE)
![Android 11+](https://img.shields.io/badge/Android-11%2B-000000?style=flat-square)
![No dependencies](https://img.shields.io/badge/dependencies-0-000000?style=flat-square)

**[⬇ Download readmeclub.apk](../../releases/latest/download/readmeclub.apk)**

</div>

---

## What's inside

| | |
|---|---|
| 📰 **News** | The latest readme.club articles, with their images, cached for offline reading. Links to other articles open right in the app. |
| 📚 **Guides** | A bookshelf of brands. Pick one, pick a guide, read it — online or not. |
| 🖼 **Wallpapers** | The gallery, filtered to images that fit your screen exactly. One tap saves the original to `Pictures/ReadmeClub`. |
| ⚙️ **Settings** | Text size, Sans or Serif, full-refresh rhythm, storage per section, and in-app updates. |

## Made for e-ink, not adapted to it

- **Pages, never scrolling.** Articles, lists and settings are cut to the screen and turned like a book.
- **One button is enough.** The capacitive button turns pages; a long press opens the menu. Tap the screen edges if you prefer.
- **Black on white, no animations.** Touch feedback is a crisp inversion, and a full refresh clears ghosting every few pages.
- **Offline first.** One sync at launch, then everything reads from the device.
- **Stays in the app.** E-ink readers rarely have a browser, so nothing ever links out.
- **Light.** Under a megabyte, pure Kotlin and native Android views, no third-party library.

Built and tested on the **Xteink S4** (Android 11, 4.3", 480 × 800). Device notes for contributors: [docs/DEVICE.md](docs/DEVICE.md).

## Install

1. Download **[readmeclub.apk](../../releases/latest/download/readmeclub.apk)** onto the reader.
2. Open it and allow installing apps from this source when Android asks.
3. That's it: later versions are offered from **Settings → About → Update**.

From a computer, `adb install -r readmeclub.apk` works too.

## Build it yourself

```sh
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds install next to the official app as *readme.club dev*. Every push is built by
GitHub Actions; a `v*` tag publishes a signed release ([docs/RELEASE.md](docs/RELEASE.md)).

## Contributing

Issues and pull requests are welcome. The house rules live in [CLAUDE.md](CLAUDE.md):
no dependency without a reason, no animation, no scrolling, no link leaving the app.

## License

[GPL-3.0](LICENSE), with additional terms in [NOTICE](NOTICE): any distributed version must
keep the attribution *“Based on readme.club for Android by Florent Bertiaux”*, and the
readme.club name, logo and icon are not licensed for use by modified versions.

<div align="center">
<sub>readme.club is independent and not affiliated with Xteink or any device brand.</sub>
</div>
