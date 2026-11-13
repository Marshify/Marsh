# Core rules (Update Part 1)

Server-side settings live in `config/grandline.json` (created on first start; missing keys get their defaults).
Every rule is server-authoritative: clients only hide the vanilla XP bar and draw the island banner.

| Key | Default | What it does |
| --- | --- | --- |
| `disableVanillaExperience` | `true` | No vanilla XP: no orbs drop, nothing gives XP, the XP bar and level number are hidden, and players' old XP is cleared when they join. Grand Line levels are unaffected. |
| `infiniteHunger` | `true` | Hunger and saturation stay full. No exhaustion, starvation or vanilla natural regeneration. |
| `baseHealthRegenPercentPerSecond` | `1.0` | Passive regeneration, as a percent of max health per second (0–100). It applies every tick, starts immediately after damage and stops at max health. `0` turns it off. |
| `disableFallDamage` | `true` | Players never take fall damage. |
| `hideAdvancementAnnouncements` | `true` | Sets the `announceAdvancements` gamerule to false on every server start. |
| `oceanDamagePercentPerSecond` | `5.0` | Fruit users in the sea lose this percent of max health each second (0–100). The damage ignores armour. `0` turns it off. |
| `oceanMinSubmersion` | `0.6` | How deep, in blocks from the feet, a fruit user must be in water before the sea counts (0.05–2). |
| `oceanOnly` | `true` | Only ocean water counts: an ocean biome (or the Grand Line Sea), with the water's surface at sea level. `false` means any water counts. |
| `oceanMaxSurfaceY` | `63` | Water whose surface is above this height is never the ocean, so fountains and hill ponds are safe. |
| `spawnIsland` | `"Millbrook Village"` | The island new players start on, and the last-resort respawn point. It must be the exact island name; an unknown name falls back to the default. |
| `islandRespawn` | `true` | Respawn order: the island the player died on, then their home island, then `spawnIsland`. A bed or respawn anchor no longer takes priority while this is on. |
| `disableWeather` | `true` | Grand Line worlds stay clear: `doWeatherCycle` is switched off and any rain or thunder is cleared within a second. |

Rain, standing next to water and boats never count as being in the sea.

## Hooks for later systems

- `HealthRegen.addModifier(player -> multiplier)` multiplies regeneration, for future races, gear or buffs.
- `OceanWeakness.addModifier(player -> multiplier)` multiplies sea damage, for future races (`0` means immune).
- `ModAttachments.HOME_ISLAND` (an island id) is the home island used by respawning. Part 2's NPCs will set it.
