# Spike Fruit (0.81.0)

Natural, Common, price 180,000. No M1, no F move, no passive, and no Elemental Reflex.
Damage is relative to Missile Fist (REF 8) before Fruit-stat and mastery scaling.

| Key | Move | Mastery | Cooldown | Energy | Damage |
|---|---|---:|---:|---:|---|
| Z | Spike Summon (tap, or hold up to 1 s) | 1 | 6 s | 20 | Central spike 100% (70% on its flank); up to 5 extra spikes at 15% each, so 130% if a target is in the central spike and two extras |
| X | Whirlwind (tap 1.5 s, or hold up to 3 s) | 25 | 10.5 s | 35 | 5 rotations × 21% = 105% tapped; 5 more × 9% = 150% held |
| C | Spiky Ball (3 s) | 50 | 13.5 s | 50 | Contact 45% + 4 drag hits × 25% = 145% at most |
| V | Spike Barrage (tap, or hold up to 3 s) | 75 | 12 s | 40 | Per spike from 35% to 75% tapped, 50% to 95% charged; the last two on one target give 110% / 145% |

## Spike Summon
- **Range and placement:** 22 blocks along the aim. The server finds real ground under the aim point; a ceiling caps how tall a spike can grow.
- **Height:** 4 blocks tapped, 7 fully charged.
- **Extra spikes:** with charge, up to 5 more spikes (2 to 4 blocks tall, 6 in total) within 4.5 blocks. A spike with no valid ground doesn't appear.
- **Damage window:** each spike stays active for 3 to 4 ticks.
- **Launch:** targets are thrown upward, harder at full charge.
- **Airborne casting:** allowed, as long as there is ground under the aim.

## Whirlwind
- **Damage:** a hit every 6 ticks, on ticks 3, 9, 15 and so on.
- **Radius:** 2.5 blocks for the first 1.5 s, growing to 5 by 3 s. The visible ring is the actual damage radius.
- **Height:** the player rises smoothly by 1.5 blocks (tapped) or 3 (held); ceilings stop the rise.
- **Movement:** horizontal movement is held at 15%.
- **Stun:** 0.7 s, shorter on resistant targets.

## Spiky Ball
- **Duration and speed:** 3 s, rolling at 0.35 blocks/tick (7 blocks/s).
- **Steering:** follows the camera heading, at most 9° per tick.
- **Collision:** swept, with one-block step-ups; walls stop it.
- **Jumping:** ground jumps only (detected from the client's jump); no air jumps.
- **Ball size:** 2.2 blocks across, with a 1.5-block contact radius.
- **Enemies:**
  - The first contact stuns.
  - Draggable enemies are carried ahead of the ball using their own collision.
  - Bosses that resist displacement take the damage but aren't moved.
  - Merchants and quest NPCs are never hit or moved.
  - Enemies are released at the end, or when they're more than 4.5 blocks away.
- **Restrictions:** no weapon attacks while rolling.
- **Recovery:** 0.5 s afterwards.
- **Ending early:** water ends it; it can't be cancelled manually.

## Spike Barrage
- **Spike count:** 4 tapped, up to 7 fully charged.
- **Range:** 14 blocks tapped, up to 26 charged.
- **Final spike height:** 5 blocks tapped, up to 8 charged.
- **Timing:** one spike every 0.12 s (2.4 ticks).
- **Steering:** after release, each new spike turns toward the current aim, at most 25° per spike. Spikes already out never move.
- **Terrain:**
  - Spikes follow the terrain, climbing or dropping up to about 2–3 blocks.
  - They don't pass through walls.
  - A large gap ends the sequence.
  - The caster needs ground underfoot.

## Instinct
- Core hits (inner half of a spike's cone) break Instinct; flank hits can be dodged.
- Whirlwind: within 70% of its radius breaks Instinct, the outer edge can be dodged.
- Spiky Ball's contacts always break it.

## Reusable systems
- **Server:**
  - `GroundSpikes`: validated ground eruptions with clearance caps, cone hitboxes, a damage window, and one hit per target.
  - The Spiky Ball's moving contact and capture system, inside `SpikeMoves` (built on `KineticBody`).
- **Client:**
  - `SpikeMesh`: faceted, flat-shaded spikes using the entity pipeline, plus the Fibonacci-lattice spiked ball.
  - Spikes that emerge from their base and sink back.
  - Arm-space spike attachments in third and first person.
  - Hiding the whole player (model, armour, items, first-person hands) while rolling.
  - Temporary crack decals.
- **Note:** spike geometry is built on the CPU each frame; it is small. Nothing is ever placed in the world.

## Tests
`GrandLineSpikeGameTests` has 8 tests: identity, tapped Summon, charged Summon, Whirlwind held radius growth, Whirlwind tap, Spiky Ball, tapped Barrage (spike 2 hit, nothing past spike 4), charged Barrage (the seventh spike hits).
