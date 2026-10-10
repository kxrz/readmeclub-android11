# The Stacks — writing content

How to write rooms, enemies, items, narrator lines and achievements for The Stacks. The
game rules are in [the-stacks.md](the-stacks.md); this page is the template for the team
and volunteers. Content is plain JSON in `app/src/main/assets/stacks/`; a unit test checks
every rule below, so a mistake fails the build instead of reaching a reader.

## Tone

Absurd, dry, literary. The narrator (the Index) is a sarcastic library catalogue that has
seen too many readers die in the returns bin. Mock situations, never people. Everything a
reader sees must fit one 4.3" screen: respect the lengths.

## Public domain

An entry that names a real author or work carries `source`:

```json
"source": { "author": "Franz Kafka", "died": 1924, "work": "The Metamorphosis", "published": 1915 }
```

The author died before 1955 and the work was published before 1930. Draw on the original
texts only, never on later adaptations. Characters from those works are fine.

## Common fields

Every entry has an `id` (lowercase, digits and `-`, unique in its file). Rooms, enemies and
items also have:

- `tiers`: `[from, to]`, the biomes it can appear in (1 = Bookshop Basement, 2 = The
  Archive; later biomes cycle back to 1 and 2 with modifiers).
- `weight`: 1 to 5, how often it is drawn among its peers (3 is normal).

Text may use `{hero}` (the hero's name), `{floor}`, `{enemy}` (the room's enemy) and
`{item}` (the item just found).

## Rooms — `rooms.json`

```json
{
  "id": "proust-waiting-room",
  "kind": "author",
  "weight": 3,
  "tiers": [1, 2],
  "name": "The Proust Waiting Room",
  "text": "A single madeleine on a plate. Eating it takes you back to a previous room.",
  "choices": [
    { "label": "Eat it", "stat": "WITS",
      "success": { "text": "You remember everything.", "effects": ["xp:20"] },
      "failure": { "text": "You remember the wrong room.", "effects": ["goto:previous"] },
      "fumble":  { "text": "You describe the plate for forty pages.", "effects": ["hp:-2"] } },
    { "label": "Don't", "stat": "SAFE",
      "success": { "text": "Wise. Also boring.", "effects": [] } }
  ],
  "source": { "author": "Marcel Proust", "died": 1922, "work": "In Search of Lost Time", "published": 1913 }
}
```

| Field | Rule |
|---|---|
| `kind` | `encounter`, `event`, `author`, `loot`, `rest` or `shop` |
| `name` | ≤ 32 characters |
| `text` | ≤ 220 characters |
| `choices` | 1 to 3 (none for `shop`: the engine fills the shop) |
| `label` | ≤ 28 characters |
| `stat` | `GUTS`, `WITS`, `LUCK` or `SAFE` (no roll, certain and modest) |
| `mod` | optional, −2 to +2: makes the check harder (+) or easier (−) |
| `success` | required; the outcome when the roll succeeds (or always, for `SAFE`) |
| `failure` | required unless `SAFE` |
| `critical` | optional, on a natural 6 (else `success` is used, with a bonus) |
| `fumble` | optional, on a natural 1: the absurd failure (else `failure`) |
| outcome `text` | ≤ 160 characters; the engine adds the roll and the gains |

- **encounter**: a monster is in the room; the engine picks one for the biome and calls
  it `{enemy}`. At least one outcome must contain the `fight` effect.
- **author**: a room named after a public-domain author or work; needs `source`.
- **loot**: something to take, often with a catch (`item:random`).
- **rest**: recover (`heal:half`), or do something else instead.

## Effects

| Effect | Meaning |
|---|---|
| `hp:+n`, `hp:-n` | Gain or lose HP (n from 1 to 10) |
| `heal:half`, `heal:full` | Recover half or all of max HP |
| `xp:n` | Gain XP (1 to 100) |
| `coins:+n`, `coins:-n` | Gain or lose coins (1 to 50) |
| `item:random`, `item:<id>` | Get a random item for the biome, or a given one |
| `lose_item` | Lose a random item (never one marked `keep`) |
| `stat:GUTS:+1` | Change a stat for good (GUTS, WITS or LUCK; ±1 only, use rarely) |
| `goto:previous` | The previous room comes back once more |
| `goto:stairs` | Skip to the stairs |
| `fight` | Fight the room's enemy (encounters); `fight:<enemy id>` for a given one |
| `damage:n` | Hurt the enemy (items used in a fight) |
| `achievement:<id>` | Unlock an achievement |
| `flag:<name>` | Remember something; achievements can count it |

## Enemies — `enemies.json`

```json
{
  "id": "overdue-notice", "name": "An Overdue Notice", "tiers": [1, 1], "weight": 3,
  "text": "A pink slip of paper, furious about a book you have never borrowed.",
  "level": 1, "hp": 8, "atk": 1, "def": 4, "resolve": 2,
  "weakness": "fine print", "loot": ["eraser-crumb"]
}
```

| Field | Rule |
|---|---|
| `name` | ≤ 32 characters |
| `text` | ≤ 160 characters, shown on the fight card |
| `level` | 1 to 10 |
| `hp`, `atk`, `def`, `resolve` | Base values for floor 1; the engine scales them by floor. Guide for a level 1 enemy: HP 6–10, ATK 1–2, DEF 3–5, RESOLVE 2–4 |
| `weakness` | A tag (≤ 16 characters) items can carry |
| `loot` | 0 to 3 item ids |
| `named` | `true` for a boss (fought before the stairs every 5th floor): 3 × HP and a `special` |
| `special` | Named enemies: `{ "name": "≤ 24", "text": "≤ 140" }`, a heavy hit every third turn |

## Items — `items.json`

```json
{
  "id": "eraser-crumb", "name": "Eraser crumb", "tiers": [1, 2], "weight": 3,
  "text": "Rubs out one mistake. Mostly yours.",
  "kind": "consumable", "rarity": "common", "price": 6,
  "effects": ["hp:+3"], "tags": ["fine print"]
}
```

| Field | Rule |
|---|---|
| `name` | ≤ 24 characters (it sits on a small button) |
| `text` | ≤ 100 characters |
| `kind` | `consumable` (used up) or `trinket` (passive while carried) |
| `rarity` | `common`, `odd`, `rare` or `absurd` |
| `price` | 1 to 99 coins |
| `effects` | Consumables: `hp`, `heal`, `damage`, `xp`, `coins`, `stat`. Trinkets: `stat` only, applied while carried |
| `tags` | Weakness tags: a consumable with the enemy's weakness deals double damage; a carried trinket with it makes Attack always hit |
| `keep` | optional `true`: never lost on death |

## Narrator — `index.json`

`{ "lines": [ { "trigger": "death", "text": "≤ 140 characters" } ] }`

Triggers: `intro`, `enter_floor`, `room`, `critical`, `fumble`, `flee`, `death`,
`level_up`, `achievement`, `boss`, `shop`, `idle_return`.

## Achievements — `achievements.json`

```json
{ "id": "died-of-mild-inconvenience", "name": "Died of Mild Inconvenience",
  "text": "Reward: a coupon for nothing. Valid nowhere.", "counter": "deaths", "at": 1 }
```

`name` ≤ 32, `text` ≤ 100. `counter` is one the engine keeps: `deaths`, `floor` (deepest
floor), `level`, `crits`, `fumbles`, `flees`, `kills`, `bosses`, `talked_down`,
`items_used`, `author_rooms`, `rests`, `shops`, `coins_spent`, or `kill:<enemy id>`,
`talk:<enemy id>`, `room:<room id>`, `flag:<name>`. Use `"counter": "none"` for one only
an `achievement:<id>` effect unlocks.

## Biomes — `biomes.json`

```json
{ "biomes": [ { "tier": 1, "name": "Bookshop Basement", "text": "≤ 160 characters" } ],
  "modifiers": [ "Damp", "Upside-down", "Second edition" ] }
```

Past the last biome, tiers cycle with a modifier added to room and enemy names
("Damp Overdue Notice") and to the difficulty.
