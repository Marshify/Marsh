# Renamed names: islands, NPCs, items and abilities (0.59.1)

Renames are **part of the mod**. They live in one file, `src/main/resources/assets/grandline/names.json`, which is built into every jar. Every player of every later version sees them, on any server or in singleplayer. Nobody else can change them: release jars have no rename command and read no config file.

## The file
```json
{
  "islands":     { "Saltmarket Docks": "Port Royal" },
  "npcs":        { "Ability Master": "Sensei" },
  "items":       { "Katana": "Wado Blade" },
  "abilities":   { "Iron Jab": "Steel Jab" },
  "island_npcs": { "Verdant Wilds": { "Bandit": "Lion", "Island Captain": "Lion King" } }
}
```
- Each entry maps an **original** English name to the new name. Delete a line to bring the original back.
- `npcs` renames that NPC or enemy everywhere. `island_npcs` renames it on one island only.
- Coverage:
  - **Islands:** chart, arrival banner, quest compass, quest text, discovery, home and race messages.
  - **NPCs and enemies:** name tags, dialogs and quest text.
  - **Items:** tooltips, shops, the Backpack and weapon moves panels.
  - **Abilities:** moves, fruits, fighting styles and techniques.

## How to change it
- Edit that file (for example on GitHub), or ask Claude.
- When the mod runs from its source folder, `/grandline names set|reset|list|reload` writes the file for you. Tab completes names. Use `set island_npc "<island>" "<npc>" <new name>` for one island. This command does not exist in release jars.

## Why it can't break the game or future code
- **Display only.** Island names stay the ids that saves, quests, island builds and travel use. Items, abilities and NPCs keep their registry ids and translation keys.
- **Layered on top.** Renames sit over `en_us.json` (`NamesLanguage`, `LanguageNamesMixin`) and never edit it.
- **Renames are never lost.** An entry whose original no longer exists stays in the file and is reported in the log.
- Enemies and NPCs refresh their names when they load, so new names reach existing worlds.

**Rules for new code:**
- Show items and abilities by translation key; renames apply automatically.
- Name an island NPC or enemy with `GrandLineNames.npc(key, islandName)`.
- Show an island name with `GrandLineNames.island(name)`, or `islandText(text)` for text that contains one.
- Never use renamed text for logic.

**Tested by `GrandLineNamesGameTests`:** every kind of rename, per-island names, internal names untouched, unmatched entries kept, and the tests never writing the real file. Also checked in game: jungle bandits shown as "Lion [Lv. 17]", other islands unchanged.
