# Races (0.27.0)

Six races. Every evolution, trial, unlock and upgrade can be done alone; nothing needs another player.

## Assignment and persistence

- New players, and saves from before races existed, get a standard race **once** on joining, weighted: Human 62.5%, Shark 12.5%, Angel 12.5%, Rabbit 12.5% (`raceWeights`).
- Cyborg and Ghoul are never rolled; they are unlocked.
- The race and all progress (stage, quests, objectives, unlocks, trial clears, points, upgrades, awakening meter) are saved with the player. They are kept through death, respawn, logout and side changes.
- Only rerolling or unlocking changes a race.
- Everything is decided on the server. The client receives its own full data, and every nearby player receives the race, stage and transformation for cosmetics.

## Races and abilities (default values)

| Race | V1 → V4 passives | V3 ability (R) | V4 awakening (Y) |
|---|---|---|---|
| Human | +5→12% damage, +2→6% speed | **Last Stand**, 12 s: up to +50% damage the lower your health (cd 70 s) | **Berserker**: +35% damage, +30% attack speed, +15% speed |
| Shark | -5→12% damage taken, Dolphin's Grace in water, water breathing and sight from V2. Sea weakness ×0.85/0.70/0.55/0.40 | **Water Body**, 10 s: -45% damage taken, sea weakness ×0.6 (cd 75 s) | **Leviathan**: -50% damage taken, a tidal wave every 3 s, sea weakness ×0.5 |
| Angel | Higher jumps, +25→75% regeneration, -5→15% move energy, glide when falling from V2 | **Heavenly Blood**, 10 s: heal 3% max health per second (cd 80 s) | **Celestial**: heal 4%/s, -15% damage taken, higher jumps, glide |
| Rabbit | +8→18% speed, higher jumps | **Agility**, 8 s: +60% speed, higher jumps (cd 50 s) | **Velocity**: +90% speed, huge leaps |
| Cyborg | -4→10% damage taken, -10→20% move energy | **Energy Core**: electric pulse (12 damage, +2% per level), +25% energy, -20% damage taken for 6 s (cd 60 s) | **Overdrive**: -30% move energy, -20% damage taken, a discharge every 2 s |
| Ghoul | 3→8% lifesteal (V1 at night only), +8→18% damage at night, -3→10% cooldowns, night vision from V2 | **Heightened Senses**, 10 s: +12% lifesteal and cooldown cut, +25% speed, nearby enemies glow (cd 70 s) | **Blood Eclipse**: +15% lifesteal, -20% cooldowns, speed, enemies revealed |

Anti-stacking rules:
- All damage reductions together are capped at 75%.
- Lifesteal heals at most 6% of max health per hit, and only on hits that land.
- Race regeneration is off while the sea is weakening a fruit user, so it never cancels ocean damage.
- Shark protection never reaches immunity: the floor is 15%.
- The V3 cooldown starts when the ability starts. A cooldown from another world clock is ignored.
- The same ability can't be used again while it is active.

## Evolution requirements

| Step | Where | Requirements |
|---|---|---|
| V1 → V2 | Race Mystic (Dawnreach Harbor) | Begin the quest, Lv 50, defeat 25 enemies, visit 3 islands, $25,000 |
| V2 → V3 | Race Mystic | Begin the quest, Lv 150, the race's objective ×40, $100,000 |
| V3 → V4 | Trial Keeper (Saltmarket Docks) | Hear the Mystic's lore at V3, find the Trial Keeper, Lv 400, an Awakening Key, clear your race's trial, $500,000 |

V3 objectives, counted only while the quest is active:
- Human: defeat enemies.
- Shark: swim, 25 blocks per point.
- Angel: heal, 1 point per heart.
- Rabbit: sprint, 25 blocks per point.
- Cyborg: use moves.
- Ghoul: defeat enemies at night.

The **Awakening Key** has a 35% chance to drop from island bosses of Lv 300 or higher when the player is V3 and has no key yet. It is used up on awakening.

## Unlockable races

- **Cyborg** (Tinkerer, Ironhold Bastion): 3 Machine Cores (8% drop from Ironhold enemies), defeat Ironhold's boss, $150,000.
- **Ghoul** (Gravekeeper, Stonewall Prison): 3 Blood Vials (8% drop from Stonewall enemies at night), defeat Stonewall's boss at night, $150,000.
- Parts go straight into the inventory of the player who earned the kill.
- Once unlocked, switching back later costs only the reroll price.

## Reroll and race changes

- The Race Mystic rerolls for **$50,000** (shown on the button, which needs a second click to confirm). It gives a random standard race, never your current one.
- The same rules apply to every race change, reroll or unlock: back to V1; quests, objectives, trial clears, Awakening Points, upgrades and the meter reset; unlocked races and unlock bosses already defeated are kept.
- You can't change race while an ability or trial is active.

## Trials (Trial Keeper)

Each player gets a private arena high in the sky, far from every island. It is built when the trial starts and removed when it ends.

| Race | Trial | Goal | Time |
|---|---|---|---|
| Human | Strength | 3 waves, then a captain | 180 s |
| Shark | Water | 3 waves in a deep pool | 180 s |
| Angel | Sky | Climb the floating platforms (a fall restarts the climb) | 100 s |
| Rabbit | Speed | Touch 8 markers in order | 50 s |
| Cyborg | Technology | Destroy 4 sentry cores while guards attack | 120 s |
| Ghoul | Darkness | 3 waves in darkness | 180 s |

- Enemies match the player's level (capped at 700).
- Dying, leaving, logging out, server shutdown or the timer running out fails the trial at no cost, and the player is returned to where they started.
- A first clear gives 3 Awakening Points, a repeat clear 1, up to 9.

## Awakening meter and upgrades

- From V4, the meter fills from damage dealt (0.6 per point) and kills (6), at most 8 per second. It doesn't fill while awakened. Y awakens at 100%, which empties the meter.
- Upgrades come in 3 tiers with 2 choices each, and you can own one per tier. A tier needs the one before it. They cost 2, 3 and 4 points, so all 9 points buy a full path.
- Each upgrade adds a fixed bonus once, so nothing stacks.

| Race | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|---|
| Human | +8% damage / +4 s | +25% gain / -10% taken | +12% damage / +4% damage always |
| Shark | -6% taken / +4 s | +25% gain / +10% damage | -8% taken / +4% damage always |
| Angel | +1%/s heal / +4 s | +25% gain / -8% taken | +1.5%/s heal / +4% damage always |
| Rabbit | +10% speed / +4 s | +25% gain / +10% damage | +15% speed / +4% damage always |
| Cyborg | -10% energy / +4 s | +25% gain / -8% taken | +6 discharge damage / +4% damage always |
| Ghoul | +3% lifesteal / +4 s | +25% gain / -10% cooldowns | +4% lifesteal / +4% damage always |

## UI, keys and visuals

- **Keys** (rebindable in Controls): **R** race ability, **Y** awaken, **U** race menu.
- **Race menu:** race and stage, passives, ability states, the next step's requirements, and the upgrade page. Talking to a race NPC opens the same menu with that NPC's actions.
- **HUD:** the race and stage, the ability timer, and the awakening meter, shown under Energy.
- **Cosmetics**, seen by everyone and growing with the stage:
  - Rabbit ears
  - Shark fin
  - Angel halo, with wings from V2
  - Cyborg visor, with shoulder lights from V2
  - Ghoul eyes, glowing at night or from V2, with tendrils while active
  - Human eyes glowing while awakened
- **Effects:** bursts for V3, V4, evolution and pulses, plus an aura while an ability is active.

## Commands (operators)

`/grandline race <player>` shows the player's race data. Subcommands:
- `set <race> <stage>`
- `energy <0-100>`
- `points <0-9>`
- `v3`
- `v4`
- `trial`

## Config (`config/grandline.json`)

- **Assignment and per-race balance:**
  - `raceWeights`
  - `races.<race>`: per-stage arrays `speed`, `damage`, `reduction`, `regen`, `lifesteal`, `jump`, `swim`, `ocean`, `energy`, `cooldown`, `night`, plus `v3Duration`, `v3Cooldown`, `v3Power`, `v4Duration`, `v4Power`
- **Evolution:** `raceEvolutionLevels`, `raceEvolutionCosts`, `raceV2Kills`, `raceV2Islands`, `raceV3Goal`
- **Unlocks, keys and reroll:** `raceUnlockParts`, `raceUnlockDropChance`, `raceUnlockCost`, `raceRerollCost`, `raceKeyBossLevel`, `raceKeyDropChance`
- **Awakening:** `awakeningEnergyPerDamage`, `awakeningEnergyPerKill`, `awakeningEnergyPerSecondCap`, `awakeningPointsFirstClear`, `awakeningPointsRepeat`, `awakeningPointsMax`

## Files

- **`race/`:**
  - `Race`, `RaceData`, `RaceService` (storage, assignment, sync)
  - `RacePassives` (passives, damage, lifesteal, sea, energy, cooldowns)
  - `RaceAbilities` (V3/V4)
  - `RaceAwakening` (meter)
  - `RaceProgress` (objectives, requirements, drops)
  - `RaceNpcs` (evolve, quests, reroll, unlock)
  - `RaceTrials`
  - `RaceUpgrades`
- **Config:** `config/RaceBalance`
- **Mixin:** `mixin/RaceDamageMixin`
- **Network:** `network/payload/Race{Sync,Look,Action}Payload`
- **Client:** `client/race/{ClientRace, RaceScreen, RaceHud}`, `client/fx/RaceFx`
- **Textures:**
  - Cosmetics: `textures/fx/race_*.png`
  - NPC skins: `textures/entity/{race_mystic, tinkerer, gravekeeper, trial_keeper}.png`
  - Items: `textures/item/{machine_core, blood_vial, awakening_key}.png`
- **Tests:** `gametest/.../GrandLineRaceGameTests`

## Tests

All 99 game tests pass: 88 earlier and 11 new race tests. The new tests cover:
- weighted assignment (16,000 rolls), no unlockable race at the start, and rerolls never repeating the current race
- assigned once; kept through side change and death copy; players independent
- damage bonus, reduction, the 75% cap, Shark sea protection (no immunity), other races at full sea weakness
- Ghoul lifesteal at night only for V1, and the 6% per-hit cap
- energy and cooldown multipliers
- V3 locked below V3, no stacking, cooldown, stale-clock handling
- the awakening meter cap, awakening only at 100%, emptied on use, no gain while awakened
- upgrade tree rules: points, tier order, one per tier, no repeats, fixed bonuses
- V2 evolution: quest, kills, islands, the right NPC, payment, cleanup
- reroll and the Cyborg unlock: cost, parts, boss, kept unlocks, switching back
- a real trial: arena built, the player teleported, the first wave spawned, return to the start point, arena removed, points 3 then capped at 9, a failure giving nothing

In-game on a dedicated server with two clients:
- Bob's cosmetics as seen by Alice for all six races, including Ghoul and Angel awakened
- the HUD and race menu
- talking to the Race Mystic, which was placed on Dawnreach in the existing world
- the Begin quest round trip
- a Rabbit trial: arena, countdown bar, then timing out back to the start point

Older Gear, Reputation and Core-rules tests now give their mock players Rabbit, which has no combat, health or energy bonus, so they still measure only their own feature.

## Known issues

- Rabbit's Velocity has speed and leaps but no separate dash move.
- Cosmetics follow the body and head angles but not limb animation. They are hidden while swimming, gliding with an elytra or sleeping, and in your own first-person view.
- The Trial Keeper is on Saltmarket Docks (Lv 375) and the V4 level is 400. Ironhold (Lv 120) and Stonewall (Lv 190) are where Cyborg and Ghoul parts drop.
- The side-choice screen reappears on every join. This is existing behaviour, unchanged here.
