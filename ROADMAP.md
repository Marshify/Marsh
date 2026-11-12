# Grand Line — Roadmap

Each milestone ends with a green `./gradlew build`, passing game tests, and an updated DEVELOPMENT_LOG.md.

## ✅ Milestone 1 — Foundation
- Fabric 26.3 project (Loom 1.19.4, Gradle 9.9.1, Java 25), split client/common source sets
- Main initializer and modular package layout
- Grand Line Crystal test item + creative tab
- Persistent, server-authoritative player profile (level, experience, currency, unallocated stat points)
- Networking foundation: profile sync, cooldown sync, validated + rate-limited client requests
- `/grandline` test, profile and admin commands; `/grandlineclient profile`
- Config file with validation
- Unit tests, server game tests, client end-to-end game test (integrated + dedicated server)

## ▶ Milestone 2 — Stats, HUD & experience sources (recommended next)
- Stat model: e.g. Melee, Defense, Sword, Gun, Fruit — allocate unallocated points via a validated C2S request
  (server checks available points; client never sends resulting values)
- Derived attributes from stats (max health, damage multipliers) applied server-side via attribute modifiers
- Client HUD: level, XP bar, currency (uses `ClientProfileCache`)
- Stats screen with allocation buttons
- XP/currency rewards from killing mobs (server-side, configurable table), with anti-farm limits
- Persistence migration path exercised (`dataVersion` 1 → 2)

## Milestone 3 — Ability framework & first fighting style
- Ability registry (data-driven definitions), slot binding stored in profile
- Keybinds → `AbilityActivationRequestPayload`; server resolves ability, checks cooldown/energy, executes
- Energy/stamina resource with sync
- One complete melee fighting style with 3–4 moves, server-side hit detection and damage
- Cooldown HUD

## Milestone 4 — Fruit system
- Fruit definitions (rarity, type), fruit items, consumption (one fruit per player), removal
- Fruit mastery progression and mastery-gated moves
- Fruit storage (per-player inventory) with persistence
- First 2–3 original fruits with full move sets

## Milestone 5 — World: oceans and islands
- Ocean-dominant world preset / dimension, island structures by level tier
- Sea travel (boats), sea hazards
- Spawn island and level-gated regions

## Milestone 6 — Quests, NPCs & enemies
- Quest-giver NPCs, kill/collect quests with server-validated rewards
- Level-tiered custom mobs and the first boss

## Later
- Weapons (swords, guns) with mastery
- Shops/economy, trading between players
- Raids, PvP rules/zones, party system
- Localization, balancing tools, server admin tooling, performance profiling
