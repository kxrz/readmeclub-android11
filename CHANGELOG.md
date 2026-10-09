# Changelog

Every user-visible change and every change to how the app is built or released is recorded
here, in the same commit as the change. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow `versionName` in `app/build.gradle.kts`.

The release workflow publishes the section of the tagged version as the GitHub release notes,
and refuses to release a version that has no section here.

## [1.0.6] - 2026-10-09

### Added
- Easter egg: a long press on the logo in About opens **Lights out**, a 5 × 5 puzzle made for
  e-ink (turn-based, one redraw per move, black and white); the best score is kept.
- **Update card** at the top of News when the launch-time check finds a newer version; tap it
  to read that version's notes before installing (from Settings → About, as before).
- **"Updated to x.y.z" card** in News after an update, until opened: it shows the new
  version's notes. A fresh install starts without it.
- **Release notes** in Settings → About: the whole changelog, paginated, readable offline.

### Changed
- The release manifest carries the version's notes (built with `jq`), and the APK ships
  CHANGELOG.md as an asset, so notes read before and after updating.
- Releases can start without pushing a tag: Actions → Android → *Run workflow* on `main`
  with the version creates the tag and the release. Both ways check the version against
  `versionName`.

## [1.0.5] - 2026-10-09

### Added
- **Folder** button on a wallpaper: pick where Save puts images (Android's folder picker),
  kept for every later save; Pictures/ReadmeClub stays the default. On a Supernote, whose
  file browser doesn't show Pictures, the first Save asks for a folder, starting at SCREENSHOT.

### Changed
- The reader always shows a bottom bar (Back, page and time left, Menu), and the diagnostic
  screen has a Back button: readers without a hardware button (Supernote, most Android
  e-readers) could not open the menu or leave without it.
- One bottom bar everywhere, with the same layout: Back bottom left, Previous before the page
  number, the main action on the right. Guides inside a brand get Back in that bar instead of
  "‹" at the top; About and a guide's contents get Previous and a page number (About's Next
  no longer wraps around); a wallpaper's bar is Back · Folder · Save, at the same height as
  the others.
- Device reports go through readme.club's contact form (no GitHub account needed); the
  GitHub issue form stays available for contributors.
- "Tested on" table in the README: Mudita Kompakt (Android 12), Supernote Nomad A6X2 and
  Manta A5X2 (Android 11, Chauvet 3) and Boox Note Air5 C (Android 15) all work.

## [1.0.4] - 2026-10-09

### Added
- **Check for updates** in Settings → About, under the version, asks GitHub again for the
  latest release without restarting the app.

### Fixed
- A failed update check now reads "Update check unavailable" instead of keeping the previous result.

## [1.0.3] - 2026-10-08

### Added
- **Continue reading**: articles and guides reopen on the page where you stopped, even after
  changing the text size; a "Continue reading · 42% read" card sits at the top of News.
- **Unread news**: articles that arrived since your last sync are bold until opened, and the
  tab reads "News · 3". A fresh install starts with nothing marked as new.
- **Guide contents**: "Contents" in the reader menu lists a guide's sections; tap one to jump to it.
- **Open on your phone**: a QR code of the readme.club page in the reader menu, to read, share
  or comment on your phone (e-readers rarely have a browser).
- **Time left** in the reader footer: "3 / 12 · 4 min left".
- **Any Android 11+ e-reader**: the wallpaper gallery finds your reader in readme.club's device
  registry (model name, then screen resolution, then aspect ratio) and falls back to all sizes.
- Page-turn keys (`PAGE_DOWN` / `PAGE_UP`) turn pages like the volume keys.
- "Off" choice for forced full refreshes, for readers that manage their own refresh modes.
- Diagnostic screen shows the app version and the device matched for wallpapers.
- GitHub "Device report" issue form and a "Tested on" table in the README.

### Changed
- **Faster first load**: news and guides show up as soon as their text is saved; images are
  then downloaded four at a time, with progress in the News card.
- The wallpaper grid and the guides shelf add columns on larger screens (unchanged on the S4);
  the default text size grows with the screen; reader lines are capped at 600 dp.
- Release notes on GitHub now come from this changelog; a tag without its section fails.

## [1.0.2] - 2026-10-08

### Added
- **In-app updates**: when a newer release exists, the tab reads "Settings •" and
  Settings → About offers "Update to x.y.z"; the APK is checked against its SHA-256 before
  Android's installer asks for confirmation. Release builds only.
- Welcome screen on first launch (logo and version).
- "Fetching the latest from readme.club…" card at the top of News while a sync runs.
- `scripts/stats.sh`: downloads and launches per release, from GitHub's public counters.

### Changed
- README rewritten with the hero photo; CLAUDE.md and docs/RELEASE.md in English.

## [1.0.1] - never released

Its changes (in-app updates) shipped in 1.0.2.

## [1.0.0] - 2026-10-08

First public release, for the Xteink S4 (Android 11).

### Added
- **News**: the 30 latest readme.club articles with header and inline images, saved for
  offline reading; paginated list and reader; links to other articles open in the app,
  external links are plain text.
- **Guides**: a bookshelf of brands, each with its guides, readable offline.
- **Wallpapers**: the gallery filtered to images that fit the screen; Save puts the original
  in `Pictures/ReadmeClub`; the submitting member and pseudo are credited.
- **Settings**: text size, Sans or Serif, full-refresh interval, sync and storage per
  section, About (promise, credits, member sign-up QR code), update check.
- E-ink design: black on white, no animations, pages instead of scrolling, the capacitive
  button turns pages (long press opens the menu), full refresh against ghosting.
- Hidden diagnostic screen (tap the version 5 times).
- Signed release builds published on GitHub Releases; GPL-3.0 with attribution terms (NOTICE).
