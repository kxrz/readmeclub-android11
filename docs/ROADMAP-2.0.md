# Road to 2.0.0

2.0.0 turns the app into a home for the club on any Android e-reader: a new interface in
the readme.club brand, six sections (News, Guides, Wallpapers, Games, Member, Settings),
games and learning that work fully offline, and an optional member account.

This file is the single place to see where we are. Tick boxes as work lands on `main`.
GitHub tracking: [Release 2.0.0](https://github.com/kxrz/readmeclub-android11/issues/1), one sub-issue per milestone.
Mockups: the 2.0 UX canvas (S4, 350 × 584 dp). Game design: [games/the-stacks.md](games/the-stacks.md).

## Release rules for 2.0.0

These hold for every milestone; a milestone is not done until they are true.

- **Small screens first.** Designed on the Xteink S4, checked on a larger reader. Touch
  targets ≥ 44 dp, body text ≥ 15 sp, one bottom bar (Back left, main action right), no
  scrolling, text paginated.
- **Offline.** Only News/Guides sync, the pack catalogue, updates and the member account
  use the network. Everything already downloaded works without it.
- **Lightweight.** No new dependency without a reason in the PR. APK under 450 KB.
  Cold start under 2 s on the S4.
- **Private.** No tracking, no identifier sent; the member account only downloads the
  member's own content.
- **Recorded.** Every change in CHANGELOG.md, in the same commit, under `[2.0.0] - Unreleased`.
- **Verified.** Green CI (debug and release builds, unit tests), then a test on the S4.

## Versions on the way

- `main` keeps `versionName` at 2.0.0 from milestone 1 on; the changelog section is
  `[2.0.0] - Unreleased`.
- At the end of each milestone, a **beta**: release workflow (Actions → Android → Run
  workflow) with version `2.0.0-beta.N`. Betas are published as pre-releases: never offered
  as in-app updates, installed by hand by testers.
- 1.0.x stays the public version until 2.0.0. A blocking bug in 1.0.x gets a 1.0.x fix
  release from a branch off its tag.
- 2.0.0 itself: release workflow with version `2.0.0`, after milestone 7.

## Milestones

Sizes: S (days), M (about a week), L (several weeks). Site work is marked **[site]**.

### M1 — Design system in code (M) · beta.1

- [x] Brand tokens as Android resources: black on white, 2 dp borders, 12 dp corners, hard shadows
- [ ] Optional terracotta accent for colour readers: moved to 2.1 (not needed for 2.0.0)
- [x] Shared views: bottom bar, button (primary, secondary), pill, sticker, window card, tile
- [x] Fonts: Space Grotesk (titles) and Space Mono (numbers) bundled; body stays the system sans
- [x] CLAUDE.md: small-screen and design-system rules (the home-hub navigation rule comes with M2)
- [x] A hidden "components" screen in the diagnostic, to check every component on a device

### M2 — Home and migrated sections (L) · beta.2

- [x] Home hub: Continue reading card, six tiles with their pills and stickers, Sync
- [x] News: list, reader, unread, update card, "Updated to" card, QR, time left (all current features)
- [x] Guides: brand shelf, contents, reader
- [x] Wallpapers: gallery that fits the device, Save, Folder, member credit
- [x] Wallpapers: **search** and **sort** (Latest, Popular, Name, Author) on the existing API
- [x] Settings: all current settings, About, Release notes, Check for updates (the Account row comes with M6)
- [ ] Larger readers: columns and margins (Nomad, Note Air5 C) — grids already adapt; to check on a large reader

### M3 — Games: Play (M) · beta.3

- [ ] Games space with Play and Learn tabs
- [ ] Lights out moved from the easter egg to Play (the long press in About stays)
- [ ] Sudoku: 6 × 6 under 400 dp, 9 × 9 above; generator with a unique solution; Easy, Medium, Hard
- [ ] Mines: 7 × 7 under 400 dp, 10 × 10 above; tap to open, long press to flag; first tap always safe
- [ ] Saves and best scores per game; resume card

### M4 — Games: Learn (L) · beta.4

- [ ] Quiz engine: packs of questions (4 answers, one right, explanation, theme), rounds of 10
- [ ] **General knowledge** pack shipped in the APK (200 questions)
- [ ] Flashcards with Leitner boxes; "Questions I missed" deck filled by quizzes
- [ ] Quiz Packs collection in the CMS **[site]**, catalogue API **[site]**
- [ ] Download, store and delete packs in the app; everything offline once downloaded
- [ ] 3 themed packs at launch (Literature, Geography, E-ink & books)

### M5 — The Stacks (L) · beta.5

- [ ] Engine, content loader, saves (spec sections 3–12), unit tests
- [ ] Screens: intro, room, outcome, fight, hero & bag, floor cleared, death, achievements
- [ ] Launch content (spec section 15), with the public-domain check
- [ ] Writing pass by the team; volunteers' template for new rooms and monsters

### M6 — Member account (L) · beta.6

- [ ] **[site]** Send a code by member number (same rate limits as today, no hint that a number exists)
- [ ] **[site]** Verify the code and return a device token (long-lived, revocable); list and revoke devices in the member area
- [ ] **[site]** Favourites, uploads, member card and decks accept the device token
- [ ] App: link (member number, code on an in-app keypad), unlink, Member screen
- [ ] My wallpapers (Favourites, Uploaded) and member card as wallpaper (light, dark)
- [ ] Privacy text on the Member screen and the /app page

### M7 — Hardening and launch (M) · 2.0.0

- [ ] Device pass: Xteink S4, Supernote Nomad and Manta, Boox Note Air5 C, Mudita Kompakt
- [ ] Budgets met: APK < 450 KB, cold start < 2 s, memory stable after 30 min of play
- [ ] Update path tested: 1.0.6 → 2.0.0 in-app update, settings and reading positions kept
- [ ] Offline audit: airplane mode on every screen
- [ ] README, docs, /app page (screenshots, What's inside, FAQ) **[site]**
- [ ] CHANGELOG `[2.0.0]` dated, release workflow `2.0.0`, launch article and Reddit post

## Dependencies

```
M1 ──► M2 ──► M3 ──► M4 ──► M5 ──► M7
               │      ▲             ▲
               │   [site] catalogue │
               └──────► M6 ◄── [site] auth
```

M1 and M2 come first: everything else is built from their components. Site work for M4
(catalogue) and M6 (auth) can start any time and run alongside the app work.

## Risks

| Risk | Answer |
|---|---|
| Scope: six sections and four games in one major | Betas per milestone; a milestone that slips can move to 2.1 (The Stacks and Member are the candidates) without blocking 2.0.0 |
| Usability on 4.3" screens | Every milestone tested on the S4 before its beta; rules enforced in CLAUDE.md |
| APK size with fonts and content | Budget checked in CI (build fails over 450 KB) |
| Writing quality for quiz and The Stacks | Content in data files from day one; writing pass by the team in M4 and M5 |
| Account security | Codes rate-limited, tokens revocable, no reading data on the server |
| Update from 1.0.x | Tested in M7; data migrations covered by unit tests |

## Status

| Milestone | State | Beta |
|---|---|---|
| [M1 Design system](https://github.com/kxrz/readmeclub-android11/issues/2) | Built, to test on the S4 | 2.0.0-beta.1 |
| [M2 Home and sections](https://github.com/kxrz/readmeclub-android11/issues/3) | Built, to test on the S4 | — |
| [M3 Games: Play](https://github.com/kxrz/readmeclub-android11/issues/4) | Not started | — |
| [M4 Games: Learn](https://github.com/kxrz/readmeclub-android11/issues/5) | Not started | — |
| [M5 The Stacks](https://github.com/kxrz/readmeclub-android11/issues/6) | Not started | — |
| [M6 Member account](https://github.com/kxrz/readmeclub-android11/issues/7) | Not started | — |
| [M7 Hardening and launch](https://github.com/kxrz/readmeclub-android11/issues/8) | Not started | — |
