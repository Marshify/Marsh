# Smoke Fruit (0.80.0)

Elemental (Logia), Common, price 100,000. No M1 and no V move.
Damage is relative to Missile Fist (REF 8) before Fruit-stat and mastery scaling.

| Key | Move | Mastery | Cooldown | Energy | Damage |
|---|---|---:|---:|---:|---|
| Z | Smoke Slam | 1 | 5.25 s | 15 | 100% (60% at the edge); range 6, radius 3.5; launches up and staggers |
| X | Smoke Blast | 20 | 9 s | 25 | 5 slashes × 40% = 200% while the target stays in the cloud |
| C | Smoke Liberation | 50 | 13.5 s | 50 | 3 hits × 50% = 150%; stun of 1.5 s, shorter on resistant targets |
| F | Smoke Bomber (hold) | 10 | 4.5 s | 50 | 30% per bomb |

## Move details
- **Smoke Blast:**
  - The projectile travels 1.5 blocks/tick out to 25 blocks.
  - The cloud stays where it forms: radius 4, lasting 2 s, with a slash every 0.4 s.
  - Each slash counts as its own hit, so invulnerability frames are cleared for that target and that hit only.
  - Anyone who walks out of the cloud stops being hit.
  - These values are fields in `SmokeMoves`.
- **Smoke Liberation:**
  - The vortex expands to radius 7 and height 10 over 0.4 s and lasts 1.5 s.
  - Each target is hit three times: on first contact, at 0.75 s and at 1.35 s.
  - It swirls targets slightly but doesn't drag them, then gives a small final push outward.
- **Smoke Bomber:**
  - Flies at about 5 blocks/second for as long as F is held, with no time limit and no energy drain.
  - Drops a smoke bomb every 0.75 s, including while hovering; each detonates with radius 2.5.
  - Releasing F, water, or any damage that actually lands ends the flight.
  - A hit negated by Elemental Reflex does not end it.

## Instinct

| Breaks Instinct | Can be dodged |
|---|---|
| Smoke Slam core (within 1.4 blocks) | Smoke Slam edges |
| The first Smoke Blast slash on the target it hit directly | The other Smoke Blast slashes |
| Smoke Liberation first contact within 3 blocks | Later and outer Smoke Liberation hits |
| Direct Smoke Bomber bomb hits | Bomb splash damage |

## Elemental Reflex (`combat/ElementalReflex`)
- **Profiles:** a fruit registers a profile with its own activation condition, attack exceptions and reaction. Only Smoke is registered so far.
  - Natural and Beast fruits never get it.
  - The other Elemental fruits stay inactive until their own reworks register a profile.
  - Blade's sword immunity stays a separate passive in `DefensivePassives`.
- **When it runs:** first in the damage pipeline (in `Dodges.evades`), before Instinct, so a negated hit never spends a dodge.
  - It works whatever the player is holding, with no energy cost, cooldown or activation.
- **What it negates:** only conventional attacks, meaning physical melee, sword or gun hits and projectiles from a living attacker.
- **What always gets through:** Aura, fruit moves, bosses, water, falls, fire, lava, the void and other environmental damage.
- **A negated hit** is cancelled at the start of `hurt`: no damage, knockback, stagger, effects, rewards or interruptions.
- **Visuals:** the struck region disperses into smoke and reforms. Visuals are limited to one per player every 3 ticks; that limit never affects immunity.

## NPC thresholds (`data/grandline/elemental_reflex/npc_thresholds.json`)
- **Override:** `config/grandline/elemental_reflex.json`, same format.
- **Keys:** stable entity type ids, never display names.
- **Rule:** a conventional NPC attack is negated only when the player's level is at least the highest of:
  - `min_player_level`;
  - the NPC's level + `level_margin`;
  - the sea floor for its `sea`.
- **Per-NPC flags:** `aura`, `bypass`, `boss` and `bypass_attacks` (damage type ids).
- **Player attacks** ignore NPC thresholds.

**These values are provisional.** The repository had no existing threshold data, so they need tuning through play:

| NPC | Player level needed |
|---|---|
| bandit, marine_trainee | NPC level or higher |
| raider | NPC level + 5 |
| corsair | NPC level + 10 |
| captain, saber_boss | Always bypasses (boss) |
| vanilla mobs | Level 1 |

Sea floors are 1, 700 and 1500.

## Reusable systems
- **Server:**
  - `ElementalReflex`: profiles, classification and NPC thresholds.
  - `DamageZones`: lingering pulse zones.
- **Client:**
  - The `SMOKE` material: the `smoke` GLSL shader, with a translucent fallback.
  - `SmokeFx.volume`: layered smoke masses with flipbook billboards.
  - `SmokeFx.vortex`: a deformed funnel.
  - `SmokeFx.limb`: connected smoke limbs.
  - `SmokeFx.hidden`: rendering-only part hiding, through the player-renderer mixin.
  - Smoke within about 8 blocks of the camera thins out, so it never fully blocks the view.

## Tests
`GrandLineSmokeGameTests` has 6 tests: identity, Elemental Reflex rules, Smoke Slam, Smoke Blast (including escaping the cloud), Smoke Liberation, Smoke Bomber.
