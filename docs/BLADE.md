# Blade Fruit (rework, 0.77.0)

The fruit keeps its id (`grandline:blade`), item, texture, Common rarity, ownership and mastery. Its dealer price is now 30,000.

| Key | Move | Mastery | Cooldown | Energy | Damage (× Missile Fist = 8) |
|---|---|---:|---:|---:|---|
| M1 | Blade Strikes (3 hits) | 1 | paced, 10 ticks per hit | 0 | 16.25% + 16.25% + 22.5% = 55% |
| Z | Razor Rush | 1 | 6 s | 15% | path cut 50% + crossing finisher 40% = 90% |
| X | Blade Dance | 20 | 10 s | 25% | 3 × 20% + final sweep 55% = 115% |
| C | Blade Barrage | 50 | 13 s | 35% | 6 × 15% + final blade 40% = 130% |
| F | Blade Flight | 80 | 7.5 s (on exit) | 40% + 0.7%/tick | none |

- **Flight speed:** about 8 blocks/s (flying speed 0.037), between Helicopter Flight (0.032) and Blast Off (0.09).
- **Blade Body passive:** blocks ordinary sword attacks.
  - What counts as a sword attack: a hit dealt inside a Sword-scaling move, or a melee hit by an attacker holding a vanilla or Grand Line sword.
  - What still lands: Aura swords (a player's Aura, or an Aura enemy), fists, fighting styles, fruit moves, guns, projectiles and the environment.
  - How: it is checked on the server against the saved fruit on every hit. No damage, knockback or stagger gets through.

## New reusable systems

- **`combat/ability/FruitStrikes`:** server-owned fruit M1 combos.
  - The client sends `FruitStrikePayload` and cancels the vanilla attack, so no punch damage is added.
- **`combat/DefensivePassives`:** damage-source passives per fruit, plus `swordAttack()` and `auraEnhanced()` classification. Uses `CombatContext.currentPower()`.
- **`AbilityHelpers`:** `assistedAim`, `inWater`, `inArc`.
- **`client/fx/BodyAttachments`:** retractable limb attachments.
  - Forearm blades for standard and slim arms, rotor blades, and a steel sheen.
  - Drawn in the arm's model space, as a third-person layer and a first-person hand mixin.
  - Every attachment times out, so nothing stays visible.
- **`client/fx/MetalMesh`:** solid steel geometry through vanilla entity render types. It is lit by the world and treated by shader packs as ordinary entity geometry.
- **`client/fx/Afterimages`:** translucent model copies with no entities.
- **`client/fx/PhysicsSparks`:** gravity, drag, bounce and velocity-stretched sparks.
- **`BodyMotion`:** new STRIKE, RUSH, DANCE and ARM_ROTOR poses.
- **`WindRender`:** materials, with a new GLSL `metal` shader. It draws:
  - a sharp edge, brushed streaks and a sliding specular band;
  - fresnel and a dissolve with a hot rim.
  - With a shader pack active, it falls back to plain blending with a baked texture.

## Verified

- **Game tests:** 9 Blade tests cover exact damage for every move, the passive block and bypass matrix, walls, flight, water and locks. The suite passes except the known-flaky `dropsInvalidTargets` enemy-AI test.
- **In the dev client:**
  - M1: forearm blades in first and third person, three distinct strikes, no vanilla punch, damage lands.
  - Deflection: sparks, sheen, health unchanged.
  - Razor Rush: dash, cut, finisher and afterimages.
  - Blade Dance: rotation, slashes and the final sweep.
  - Blade Barrage: alternating throws, solid steel blades and impacts.
  - Blade Flight: raised-arm rotors, flight, landing and retraction.

## Not done or not verified

- **Shader packs:** Iris/BSL were not tested (not installed here).
- **Distortion and soft particles:** screen-space distortion and depth-aware soft particles are not implemented.
- **Lodestone:** not added.
- **Not tested live:** several users at once, and death or dimension change in the middle of a move (only the cleanup paths exist).
- **The new sounds:** you haven't listened to them yet.
