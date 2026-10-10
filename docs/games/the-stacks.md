# The Stacks — game specification

An endless, absurd text dungeon under a bookshop, built for e-ink readers. Part of
Games → Play, shipped in 2.0.0. Mockups: the "The Stacks" row of the 2.0 UX canvas.

## 1. Pillars

1. **One screen, one step.** A room, a fight turn or an event fits on one S4 screen
   (350 × 584 dp) with its choices. No scrolling, no animation, one redraw per tap.
2. **Short sessions, no ending.** A floor takes about five minutes. The game saves after
   every tap and never ends: floors get harder and stranger.
3. **Absurd and literary.** A sarcastic narrator ("the Index"), rooms and monsters drawn
   from public-domain literature, achievements with ridiculous titles.
4. **Fully offline.** Everything is generated on the device from text tables in the APK.
   Extra content comes as packs, downloaded once.
5. **Content without code.** Rooms, monsters, items, lines and achievements live in data
   files the team and volunteers can write.

## 2. Screens

All screens follow the app rules (CLAUDE.md): one bottom bar, Back bottom left, main
action bottom right, touch targets ≥ 44 dp, body text ≥ 15 sp.

| Screen | Content | Bottom bar |
|---|---|---|
| **Intro** (first run only) | Opening text by the Index, class choice (3 buttons) | Back · Start |
| **Room** | Header: floor, room name; status line `HP 14/20 · Lv 4 · 37 coins`; Index box (optional, one line); room text; 1–3 choices, each with the stat it tests | Back · "Autosaved" · Hero & bag |
| **Outcome** | What happened (≤ 2 sentences), gains and losses as pills (`+12 XP`, `−3 HP`, `Biscuit`) | Back · Continue |
| **Fight** | Enemy card (name, level, weakness, HP as 10 squares), last two turns in one sentence each, 4 actions in a 2 × 2 grid | Back · status |
| **Hero & bag** | Level and XP bar, GUTS / WITS / LUCK, bag (6 slots, tap to use or drop), latest achievement | Back · Continue |
| **Floor cleared** | Floor summary, narrator line, full e-ink refresh | Back · Go down |
| **Death** | How you died (one line), what you keep and lose, narrator line | Back · Try again |
| **Achievements** | Paginated list, unlocked ones in bold, locked ones as "???" | Back · Previous · n / N · Next |

Choices are full-width buttons 52 dp tall (fight actions 56 dp). A choice label is at
most 28 characters; the stat tag (`WITS`, `GUTS`, `LUCK`, `SAFE`) sits on its right.

Larger screens (≥ 600 dp wide) keep the same structure with larger text and margins;
the fight actions may sit in one row of four. Targets never shrink.

Page keys: on Room and Outcome screens the page-down key triggers the main action
(Continue); in a fight it does nothing (a misread key press must never attack).

## 3. Core loop

```
Floor N ── room 1 … room 6–8 ── stairs ── Floor N+1
              │
              └─ each room: read → choose → outcome (→ fight) → next room
```

A floor is generated when entered: 6 to 8 rooms drawn from the room table, then stairs.
Every 5th floor ends with a **named enemy** (a "boss") before the stairs.

Room kinds and weights (floors 1–5; later tiers can change them):

| Kind | Weight | What happens |
|---|---|---|
| Encounter | 35 | A monster; fight, talk or flee |
| Event | 30 | A situation with 2–3 choices and stat checks |
| Author room | 10 | A named room (Proust, Kafka…), an event with a special reward |
| Loot | 10 | An item, sometimes with a catch |
| Rest | 10 | Recover HP (choose: rest, or search for loot instead) |
| Shop | 5 | Spend coins on 3 random items |

The same room never appears twice on a floor.

## 4. Character

- **Classes** (chosen once, at the start):
  - *Margin Scribbler*: WITS 3, GUTS 1, LUCK 2.
  - *Spine Breaker*: GUTS 3, WITS 1, LUCK 2.
  - *Speed Reader*: LUCK 3, GUTS 2, WITS 1.
- **Max HP** = 10 + 2 × GUTS + 2 × level.
- **Level up** at 50 × level XP: +1 stat point (player picks), full heal, narrator line.
- **Coins**: found in rooms and on enemies, spent in shops.
- **Bag**: 6 slots. Consumables are used up; trinkets give a passive effect while carried.
  A full bag asks which item to drop.
- A linked member account shows the member number as the hero's name ("Reader #042");
  otherwise "Reader". Nothing is sent anywhere.

## 5. Checks

Every uncertain action is one roll:

> **d6 + stat ≥ difficulty**

- Difficulty = 4 + ⌊floor ÷ 3⌋, capped at 9; an entry can add or subtract up to 2.
- A natural **6** always succeeds and is a **critical** (better outcome, sticker "Critical!").
- A natural **1** always fails and picks the entry's **absurd failure** text.
- `SAFE` choices have no roll and a modest, certain outcome.

The roll is shown in the outcome text ("You roll a 5 + WITS 3 = 8."), so players learn
the rules by playing.

## 6. Fights

Turn order: the player acts, then the enemy (if still standing).

| Action | Roll | Success | Failure |
|---|---|---|---|
| **Attack** | d6 + GUTS vs enemy DEF | Damage = 1 + (total − DEF), crit doubles | Miss |
| **Talk it down** | d6 + WITS vs enemy RESOLVE | Enemy RESOLVE −1 per success; at 0 it leaves and drops its loot | It is offended: next enemy hit +1 |
| **Use an item** | none | Item effect | — |
| **Flee** | d6 + LUCK vs 4 + ⌊enemy level ÷ 2⌋ | Back to the previous room, no reward | Enemy gets a free hit |

Enemy turn: d6 + ATK vs 4 + GUTS; on success, damage = ATK (×2 on a natural 6).

**Weakness tags** (e.g. `fine print`, `daylight`, `deduction`): an item or a choice with
the matching tag deals double damage or succeeds automatically. The fight screen always
shows the weakness, so the right item is a choice, not a guess.

**Enemy stats** scale with the floor: each stat = base × (1 + 0.15 × (floor − 1)),
rounded. Named enemies have 3 × HP and one special move.

Rewards: XP = 10 × enemy level, coins = d6 × level, plus the enemy's loot table.

## 7. Death and checkpoints

- At 0 HP: the Death screen. The run restarts **at the first room of the current floor**.
- Kept: level, XP, stats, achievements, deaths counter.
- Lost: half the coins and one random item (never a trinket marked `keep`).
- No game over, no lives. The narrator keeps count.

## 8. Endless progression

Floors are grouped in tiers of five. Each tier unlocks its own tables (a "biome"):

| Floors | Biome | Flavour |
|---|---|---|
| 1–5 | Bookshop Basement | Returns, receipts, lost bookmarks |
| 6–10 | The Archive | Damp, filing, the Overdue Librarian |
| 11–15 | The Bindery | Glue, spines, paper cuts |
| 16–20 | Footnote Depths | Everything is smaller and angrier |

Past the last biome, tiers cycle with **modifiers** added to every name and difficulty
("Damp", "Upside-down", "Second edition"), so content never runs out.

## 9. The Index (narrator)

Lines are drawn from tables keyed by trigger, never the same line twice in a row:

`intro`, `enter_floor`, `room`, `critical`, `fumble`, `flee`, `death`, `level_up`,
`achievement`, `boss`, `shop`, `idle_return` (app reopened after a day or more).

A line is at most 140 characters. The Index box appears on at most one room in three, so
it stays a treat rather than noise.

## 10. Achievements

About 30 at launch, defined in data like everything else. Conditions are counters the
engine already keeps: deaths, floors, crits, fumbles, flees, items used, enemies talked
down, named rooms visited, specific enemies beaten with a specific action. Unlocking one
shows the "Achievement" sticker on the next screen and a narrator line.

## 11. Content model

Content ships as JSON in `app/src/main/assets/stacks/` and in downloadable packs, with one
file per table. Every entry has an `id`, a `weight`, a `tiers` range and optional `tags`.

```json
{
  "id": "proust-waiting-room",
  "kind": "author",
  "weight": 3,
  "tiers": [1, 99],
  "name": "The Proust Waiting Room",
  "text": "A single madeleine on a plate. Eating it takes you back to a previous room.",
  "choices": [
    { "label": "Eat it", "stat": "WITS",
      "success": { "text": "You remember everything. +20 XP.", "effects": ["xp:20"] },
      "failure": { "text": "You remember the wrong room.", "effects": ["goto:previous"] },
      "fumble":  { "text": "You describe the plate for forty pages. −2 HP.", "effects": ["hp:-2"] } },
    { "label": "Don't", "stat": "SAFE",
      "success": { "text": "Wise. Also boring.", "effects": [] } }
  ],
  "source": { "author": "Marcel Proust", "died": 1922, "work": "In Search of Lost Time", "published": 1913 }
}
```

- **Effects** are a small fixed vocabulary: `hp:±n`, `xp:n`, `coins:±n`, `item:<id>`,
  `lose_item`, `stat:<STAT>:±n`, `goto:previous|stairs`, `fight:<enemy id>`,
  `achievement:<id>`, `flag:<name>`. The engine rejects unknown effects when loading.
- **Text** may use `{hero}`, `{floor}`, `{enemy}`, `{item}` placeholders.
- **Lengths** are checked when loading (and in a unit test over the shipped files):
  room text ≤ 220 characters, outcome ≤ 160, choice label ≤ 28, narrator line ≤ 140.
- **Public domain**: an entry citing a real author or work must carry `source`, with the
  author's death year before 1955 and the work published before 1930. A unit test enforces
  it. Draw on the original texts only, never on adaptations still under copyright; mock
  situations, not the authors.

Tables: `rooms.json`, `enemies.json`, `items.json`, `index.json` (narrator lines),
`achievements.json`, `biomes.json`.

Enemy entry: `id`, `name`, `text`, `level`, `hp`, `atk`, `def`, `resolve`, `weakness`,
`loot` (weighted item ids), `special` (named enemies only), `source`.

Item entry: `id`, `name`, `text`, `kind` (`consumable` | `trinket`), `rarity`
(`common` | `odd` | `rare` | `absurd`), `effects`, `tags` (weakness matches), `price`.

## 12. Randomness and saves

- One seeded random generator per run. The seed and a counter are saved, so a run
  resumes exactly and tests are deterministic.
- The save is one JSON file in the app's private storage (`stacks-save.json`, schema
  version inside), written after every action. It holds: class, stats, level, XP, HP,
  coins, bag, floor, the floor's room list and position, flags, counters, achievements,
  last narrator lines. Under 10 KB.
- A save from an older schema is migrated or, failing that, kept aside and the player
  starts a new run with achievements kept.

## 13. Packs

A **biome pack** has the same tables plus `pack.json` (`id`, `title`, `version`,
`tiers`). It is listed in the same catalogue as quiz packs, downloaded once, stored in the
app's files and works offline. Removing a pack never breaks a save: rooms from a missing
pack are replaced when the floor is regenerated.

## 14. Architecture

Package `club.readme.android.game.stacks`:

| Class | Role | Android? |
|---|---|---|
| `StacksContent` | Loads and validates the tables (APK assets + packs) | No (reads strings) |
| `StacksEngine` | State, floor generation, checks, fights, effects, achievements | No |
| `StacksSave` | JSON (de)serialisation with `org.json` | No |
| `StacksActivity` | Screens from the engine's state, bottom bar, page keys, refresh | Yes |

The engine is pure Kotlin and fully unit tested: checks and crits, fight resolution,
scaling, death and checkpoint, effects, achievements, save round-trip, determinism with a
fixed seed, and validation of every shipped table (lengths, effects, public domain).

No new dependency. Estimated size: about 40 KB of code and 50–80 KB of text for the base
content.

## 15. Launch content

| Table | Count |
|---|---|
| Rooms (all kinds) | 60, including 12 author rooms |
| Enemies | 25, including 4 named enemies |
| Items | 30 |
| Narrator lines | 80 |
| Achievements | 30 |
| Biomes | 2 complete (floors 1–10), then modifiers |

## 16. Out of scope for 2.0.0

Images and sound, online leaderboards, multiplayer, saving across devices, and writing
packs on the website (the content format is ready for it).

## 17. Open questions

- Final name: "The Stacks" (working title).
- Should a linked member account unlock a cosmetic title ("Member #042, Founding Reader")?
- Rest rooms: full heal, or half, to keep tension on later floors?
