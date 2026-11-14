# Update batch 3: rewards, teams, fishing, economy, adventure mode, navigation

## Kill credit (`combat/CombatCredit.java`)
- Every time a player damages something (melee, a move, a projectile they own), that player is remembered as its attacker.
- A death with no attacker of its own counts for the last player who hit within `killCreditSeconds`. That covers fire, a fall after a knockback, drowning, lingering areas and the void.
- Policy: the most recent player hit wins.
- A death caused by another creature, or long after the last player hit, credits no one.
- Each death pays out once: experience, currency, quest progress, mastery and reputation.

## New-quest cards (`quest/QuestUnlocks.java`)
- Every non-starter island with quests opens at its registry level. The first time a player reaches that level, they get a card ("NEW QUEST AVAILABLE!", the island, the required level) plus a chat line.
- Seen islands are saved per player, so a card never repeats.
- Players who passed a level before this update are marked as told when they first join, so they don't get a flood of cards.

## Team starting islands (`world/IslandSpawns.java`)
| Side | Starting island |
| --- | --- |
| Pirates (and anyone who hasn't picked a side) | `spawnIsland` (Millbrook Village) |
| Marines | `marineSpawnIsland` (Redbrick Port) |

- **Redbrick Port is now Lv. 1**, with Lv. 1–2 enemies and beginner quests. It has an Innkeeper, a Swordsmith, a Fisherman and an arrival spot on the quay.
- A brand-new player is moved to the Marines' island when they first pick the Marines.
- Players who have played before are never moved by picking or changing a side.
- **Respawning:** the island you died on, then your home, then your side's starting island.

## Keep inventory (`player/rules/KeepInventory.java`, `PlayerKeepInventoryMixin`)
- Nothing drops on death. The same stacks (with their names, durability and attributes) move to the respawned player, so items can be neither lost nor duplicated.
- This works whatever the `keepInventory` gamerule is set to.
- Levels, currency, mastery and equipped gear already survive death as saved player data.

## Fishing (`fishing/*`)
- **Starter Fishing Rod:** your pixel art, rebuilt as a 64 × 64 texture and held like a rod. The Fisherman sells it for `fishingRodPrice`.
- **How it plays:**
  1. Right-click to cast. The bobber arcs out and floats; casting onto land fails with a message.
  2. A fish bites after a wait: bubbles, a splash, a bell, and the bobber dips.
  3. Right-click within the hook window to hook it.
- **Catch minigame:** the fish darts up and down a water bar in a pattern that depends on its species. You hold the mouse, Space or W to lift the green net, and it sinks when you let go. The reel meter fills while the fish is in the net and drains while it's out. Full means caught; empty means it got away. Escape lets it go.
- **Anti-cheat:** the server runs the fight from your hold and release inputs, so a catch can't be faked. Each fight pays out once. Disconnecting, dying, putting the rod away or losing the bobber ends it safely.
- **Fish:**

  | Fish | Tier | Difficulty | Unlocks at mastery | Where |
  | --- | --- | --- | --- | --- |
  | Silverfin | Common | 1.0 | 1 | anywhere |
  | Reef Snapper | Common | 1.6 | 1 | anywhere |
  | Sunscale Tang | Uncommon | 2.2 | 3 | anywhere |
  | Storm Eel | Uncommon | 3.0 | 6 | anywhere |
  | Glass Squid | Rare | 3.2 | 10 | anywhere |
  | Ember Koi | Rare | 3.6 | 14 | anywhere |
  | Abyss Angler | Epic | 4.4 | 20 | open sea (no island within 120 blocks) |
  | Crown Marlin | Legendary | 5.0 | 30 | open sea, Second Sea onward |

  Rarer fish get likelier with mastery, in open water and in later seas. Fish sell to the Fisherman as before.
- **Fishing Mastery (Lv. 1–100):**
  - Earned per catch; rarer and harder fish teach more.
  - Higher mastery gives a bigger net, faster bites, a longer hook window, a slower-draining meter and access to rarer fish.
  - Saved through death, logout and restarts.
  - Shown in the minigame panel and the Fisherman's dialog.
- **Animations:** casting and reeling poses (everyone nearby sees them), the line drawn from the rod tip, and splash and bubble effects.

## Prices
Income check: a Lv. 1 quest pays about $65, plus 6 kills at about $11 each, so roughly $130 per starter quest.

| Item | Before | Now | Config |
| --- | --- | --- | --- |
| Cutlass | $2,500 | $300 (about 2–3 quests) | `swordPrices[1]` |
| Katana | $8,000 | $500 (about 4 quests) | `swordPrices[0]` |
| Slingshot | $1,500 | $400 | `gunPrices[0]` |
| Musket | $12,000 | $6,000 (about 15 Verdant-level quests) | `gunPrices[1]` |
| Starter Fishing Rod | — | $150 | `fishingRodPrice` |

- Configs still holding the old default prices are moved to the new ones; prices a server set itself are kept.
- Both starting islands have a Swordsmith with the same stock and prices.

## Adventure mode (`world/AdventureRules.java` plus mixins)
In Grand Line worlds, regular players can't:
- break or place blocks (no block drops follow);
- open containers;
- use crafting, smelting, anvils, grindstones, smithing tables, stonecutters, enchanting tables, brewing stands or lecterns;
- toggle doors, trapdoors, gates, buttons or levers;
- use or break item frames, paintings, armour stands or storage carts;
- craft, including the 2×2 inventory grid;
- pick up non-Grand Line items.

Also in Grand Line worlds:
- Explosions break no blocks.
- Fire spread and mob griefing are off.
- Vanilla creatures drop no loot.

NPCs, shops, quests, combat, fishing and every Grand Line screen still work. **Bypass:** operators at `adventureBypassPermission` who are in creative mode, or tagged `grandline_build` (`/tag <player> add grandline_build`).

## Quest navigation (`quest/QuestNavigation.java`, client `QuestCompass`, `ClientNavigation`)
- **Compass (top right):** the needle turns toward the quest giver as you move and look around. Below it: the target's name, island and distance.
- **Target:**
  - With an active quest: that quest's island.
  - Otherwise: the highest-level island with quests you've reached, in your sea. That's your side's starting island until Lv. 15.
- **Outline:** the target quest giver gets a gold outline, visible through walls, for you alone. It isn't the glowing effect, so other players don't see it.
- **Toggle:** **N**. The choice is saved per player.
- Distant or unloaded quest givers are pointed at by position, without loading their chunks.

## Configuration (`config/grandline.json`)
| Key | Default |
| --- | --- |
| `killCreditSeconds` | 15 |
| `keepInventoryOnDeath` | true |
| `marineSpawnIsland` | "Redbrick Port" |
| `fishingRodPrice` | 150 |
| `fishingBiteMinSeconds` / `fishingBiteMaxSeconds` | 4 / 12 |
| `fishingMasteryXpMultiplier` | 1.0 |
| `fishWeights`, `fishMinMastery` | overrides by fish id, e.g. `"storm_eel": 5` |
| `swordPrices` | [500, 300] |
| `gunPrices` | [400, 6000] |
| `adventureMode` | true |
| `adventureModeAllWorlds` | false |
| `adventureBypassPermission` | 2 |
| `allowedBlockInteractions` | [] |
| `allowedItems` | [] |
