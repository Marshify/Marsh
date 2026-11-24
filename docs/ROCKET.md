# Rocket Fruit (rework, 0.75.0)

The fruit's id (`grandline:rocket`), item, Common rarity, 5,000 price, ownership and mastery are unchanged.

| Key | Move | Mastery | Cooldown | Energy | Damage |
|---|---|---:|---:|---:|---|
| Z | Missile Fist | 1 | 7.5 s | 15% | 100% (blast 2.6 blocks, 60% at the rim) |
| X | Air Strike | 20 | 10.5 s | 25% | 5 rockets × 40%; a target takes at most 3, so 120% |
| C | Rocket Crash | 50 | 14.25 s | 35% | 140% (blast 4.2 blocks) |
| V | unused | – | – | – | Blast Off keeps the movement key |
| F | Blast Off | 75 | 7.5 s (on landing) | 35% + 1.2%/tick | none: sustained flight, up to 10 s |

100% = 8 base damage before mastery and Fruit-stat scaling. Energy is a share of max energy, like every other move.

## How it works

- **Server authority.** `fruit/rocket/RocketMoves.java` runs each move as a timeline (windup, launch, travel, impact). Hits, damage, explosions and the crash path are decided on the server; the server sends one visual beat per step. Rocket Crash simulates the body's path on the server and corrects the player's position at impact.
- **Cleanup.** A crash ends cleanly on death, logout or a second crash. Flight uses the shared `FlightService`, which ends on logout, server stop, an empty energy bar or the time limit, and grants slow falling on landing. All client effects end when their caster is gone.
- **Client.** `client/fx/RocketFx.java` draws:
  - a 3D rocket model built from quads every frame (8-sided hull with panel texture `textures/fx/rocket_hull.png`, painted nose cone, four fins, dark nozzle, face shading from a fixed light);
  - an additive exhaust (white core, flame tongues, glow);
  - smoke ribbon trails with a hot streak near the nozzle;
  - explosions (flash, fireball lobes, sparks, embers, smoke), ground shockwave rings, the Air Strike target marker;
  - twin back thrusters while flying;
  - poses and camera shake by distance.
- **Quality presets.** The "Effect detail" setting now has Low, Medium, High and Ultra. It scales sparks, smoke, embers and trail length. The rocket, exhaust core and blast shapes always show.
- **Sounds.** 12 cues of its own (ElevenLabs, levelled like Flame's; see AUDIO.md).
- **Unused slot.** `AbilityDef.empty()` marks a slot with no move. It is skipped in the HUD, tooltips, the fruit shop and mastery messages, and activating it is rejected.

## Implemented, fallback, unsupported

- **Implemented and verified in game:** all four moves, in a dev client. Each was cast, recorded and checked frame by frame:
  - Missile Fist: rocket model in flight, explosion, damage.
  - Air Strike: five rockets dive onto the mark.
  - Rocket Crash: windup, launch, explosion, damage.
  - Blast Off: thrusters, flight, landing.
  - The HUD shows four moves with F for Blast Off.
- **Game tests:** 6 tests in `GrandLineRocketGameTests` cover identity and numbers, each move, mastery locks and the unused slot. The full suite (197 tests) passes.
- **Fallbacks:**
  - Advanced GLSL shader effects (custom shader programs, heat distortion, bloom) are not implemented. Glow uses additive blending in Grand Line's existing render types, which is the fallback.
  - Shader packs (Iris/BSL) were not tested: none is available in this build environment. The effects only use the vanilla render pipeline, so they should draw under shader packs, but their look there is unverified.
- **Not added:** Lodestone. It would become a new required library for every player. The effects above did not need it, and no build of it was tested here.
