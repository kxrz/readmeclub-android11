<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/logo-dark.png">
  <img src="docs/assets/logo-light.png" alt="readme.club" width="240">
</picture>

## Your e-ink reader just got its own club.

**readme.club is the first e-ink media with its own app for e-readers.**<br>
News, guides and thousands of wallpapers — right on the device, offline, in glorious black and white.

[![Latest release](https://img.shields.io/github/v/release/kxrz/readmeclub-android11?style=flat-square&label=release&color=000000)](../../releases/latest)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-000000?style=flat-square)](LICENSE)
![Android 11+](https://img.shields.io/badge/Android-11%2B-000000?style=flat-square)
![APK size](https://img.shields.io/badge/APK-~160%20KB-000000?style=flat-square)
![Dependencies](https://img.shields.io/badge/dependencies-0-000000?style=flat-square)

### [Download readmeclub.apk](../../releases/latest/download/readmeclub.apk)

<img src="docs/assets/hero.jpg" alt="The readme.club app on an Xteink S4, showing the wallpapers that fit its screen" width="760">

<sub>The Wallpapers tab on an Xteink S4 — 700+ pages of wallpapers that fit the screen pixel for pixel.</sub>

</div>

---

## What's inside

| | |
|---|---|
| **News** | Fresh readme.club articles with their photos, saved for offline reading. New ones stand out in bold, and you pick up exactly where you stopped. Want it on your phone? Scan the QR code from the reader menu. |
| **Guides** | A real bookshelf: pick a brand, pick a guide, jump to any step from its contents — even with Wi-Fi off. |
| **Wallpapers** | 4,000+ community wallpapers, filtered to fit your screen exactly. One tap and the original lands in `Pictures/ReadmeClub`. |
| **Settings** | Text size, Sans or Serif, refresh rhythm, storage per section — and one-tap updates. |

## Made for e-ink, not squeezed onto it

- **Pages, never scrolling.** Everything is cut to the screen and turned like a book, with the minutes left in the footer.
- **One button is all you need.** The capacitive button turns pages, a long press opens the menu. Prefer tapping? The screen edges work too.
- **Crisp black on white.** No animations, no spinners; touch feedback is a clean inversion, and a full refresh wipes ghosting every few pages.
- **Offline first.** One sync when you open the app, then everything reads from the device.
- **Never leaves the app.** E-readers rarely have a browser, so nothing ever links out.
- **Featherweight.** About 160 KB. Pure Kotlin and native Android views, no library dependency (just a small MIT QR code generator vendored in the source).

Built and loved on the **Xteink S4** (Android 11, 4.3", 480 × 800), and made for **any
Android 11+ e-reader**: the layout grows with the screen, the wallpaper gallery matches your
display through readme.club's device registry, volume and page-turn buttons both turn pages,
and forced refreshes can be switched off on readers with their own refresh modes.
Device notes: [docs/DEVICE.md](docs/DEVICE.md).

### Tested on

| Device | Android | Status |
|---|---|---|
| Xteink S4 | 11 | Reference device |

Running it on another reader? **[Send a device report](../../issues/new?template=device-report.yml)** —
two minutes, and your reader joins the list.

## Install in three steps

1. Download **[readmeclub.apk](../../releases/latest/download/readmeclub.apk)** onto your reader.
2. Open it and allow installing apps from this source when Android asks.
3. Enjoy. New versions show up as **Settings •** and install from **About → Update**.

Got a computer handy? `adb install -r readmeclub.apk` works too.

## Build it yourself

```sh
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

What changed in each version: [CHANGELOG.md](CHANGELOG.md).

Debug builds install next to the official app as *readme.club dev*. Every push is built by
GitHub Actions; a `v*` tag publishes a signed release ([docs/RELEASE.md](docs/RELEASE.md)).

## Join in

Ideas, bug reports and pull requests are all welcome — this app grows with the community,
just like the site. The house rules live in [CLAUDE.md](CLAUDE.md): no dependency without a
reason, no animation, no scrolling, no link leaving the app, everything in English.

Not on the club yet? Grab a free member account at **[readme.club/member/join](https://www.readme.club/member/join)**

## License

[GPL-3.0](LICENSE), with additional terms in [NOTICE](NOTICE): any distributed version must
keep the attribution *“Based on readme.club for Android by Florent Bertiaux”*, and the
readme.club name, logo and icon are not licensed for use by modified versions.

<div align="center">
<br>
<sub>Made by the readme.club team, its volunteers and every reader who drops by.<br>
readme.club is independent and not affiliated with Xteink or any device brand.</sub>
</div>
