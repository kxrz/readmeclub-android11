# Xteink S4 — notes for contributors

What the app relies on, measured on the device.

| | |
|---|---|
| System | Android 11 (API 30), no Google Play Services needed |
| SoC | Rockchip RK3326, 32-bit userland (`armeabi-v7a`), 2 GB RAM |
| Screen | 4.3" e-ink, 480 × 800 portrait, 219 dpi → **350 × 584 dp** (density 1.36875) |
| Browser | None — the app never links outside itself |

## Capacitive button

- Reported as **`KEYCODE_VOLUME_DOWN` (25)**, scan code 114.
- A long press is *not* flagged by `KeyEvent.isLongPress`: it arrives as repeated
  `ACTION_DOWN` events with a growing `repeatCount`, then one `ACTION_UP`. Detect it by
  hold time (`eventTime - downTime`), see `eink/PageKeys.kt`.
- The app consumes the key everywhere, so pressing it never changes the volume.

## E-ink refresh

- No vendor refresh API is used. A full-screen black then white frame (~120 ms,
  `eink/FullRefresh.kt`) clears ghosting reliably.
- No animations anywhere; touch feedback is a black/white inversion.

## Other readers

The app targets any Android 11+ device. The wallpaper gallery picks this reader's entry in
readme.club's device registry (`/api/devices`): by model name first, then exact screen
resolution, then same aspect ratio (`data/DeviceMatch.kt`); with no match it shows every
size. `KEYCODE_PAGE_DOWN` / `KEYCODE_PAGE_UP` turn pages like the volume keys.

## Wallpapers

- `WallpaperManager` has no visible effect on the S4. The app saves wallpapers to
  `Pictures/ReadmeClub` instead.

## Development tips

- `adb` over Wi-Fi works well (Developer options → Wireless debugging); the port changes
  after each reboot. Some USB cables only charge the device.
- Logcat shows SELinux denials for `sys.gmali.version` from the Mali GPU driver at
  launch: harmless, every app triggers them.
