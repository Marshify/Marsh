# Renaming islands, NPCs, items and abilities (0.59.0)

## How to rename (server operators)

In game (Tab completes every name):
```
/grandline names set island "Saltmarket Docks" Port Royal
/grandline names set npc "Ability Master" Sensei
/grandline names set item Katana Wado Blade
/grandline names set ability "Iron Jab" Steel Jab
/grandline names list island            what is renamed
/grandline names reset item Katana      give back the original name (the new name works too)
/grandline names reload                 after editing the file by hand
```
Names with spaces go in quotes. Renames are saved straight away in `config/grandline-names.json` (on a server, the
server's config folder), keep across restarts and mod updates, and are sent to every player on join and on every change.

The file can be edited by hand too; each section maps an **original** name to the name you want:
```json
"islands":   { "Saltmarket Docks": "Port Royal" },
"npcs":      { "Ability Master": "Sensei" },
"items":     { "Katana": "Wado Blade" },
"abilities": { "Iron Jab": "Steel Jab" }
```

Covers:
- **Islands:** chart, arrival banner, quest compass and quest text, discovery, home and race messages.
- **NPCs and enemies:** name tags and dialogs.
- **Items:** tooltips, shops, the Backpack and the moves panel title for weapons.
- **Abilities:** move names, fruits, fighting styles and techniques.

Renaming one name changes it everywhere it's used, for example both Ability Masters.

## Why it can't break the game or future code

- **Display only.** Nothing inside the game changes:
  - island names stay the ids that saves, quests, island builds, treasure spots and travel use;
  - items, abilities and NPCs keep their registry ids and translation keys;
  - saves, worlds and other servers are unaffected.
- **Layered on top.** Renames sit over the mod's English text (`NamesLanguage`, installed by `LanguageNamesMixin`) and never edit `en_us.json`. The mod can keep adding or changing text freely.
- **Keyed by the original name, and never lost.** If a later version renames something, the old entry stays in the file and the log warns `no item is called "..."`. Point it at the new name (or reset it).

**Rule for new code:**
- Show items, abilities and NPCs through their translation key, which the game already does, so renames apply automatically.
- Show an island name with `GrandLineNames.island(name)`, or `GrandLineNames.islandText(text)` for text that contains one.
- Never use the renamed text for logic. Compare ids and original island names, as now.

**Tested by `GrandLineNamesGameTests`:** one rename of each kind; internal names untouched; an unmatched entry kept but unused; reset. Also checked in game.
