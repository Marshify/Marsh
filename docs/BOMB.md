# Bomb Fruit (0.79.0)

Natural, Common, price 80,000. Damage is relative to Missile Fist (REF 8) before Fruit-stat and mastery scaling.
No explosion changes terrain: craters, scorch marks and debris are client-side cosmetics.

| Key | Move | Mastery | Cooldown | Energy | Damage |
|---|---|---:|---:|---:|---|
| M1 | Blast Toss (tap / hold to 1.5 s) | – | 3 s | 5 | 45% to 75%; radius 2.5 to 4; about 30 blocks of arc |
| Z | Targeted Bomb (tap / hold to aim, up to 3 s) | 1 | 4.5 s | 15 | 100%; radius 4; range 18; launches targets up |
| X | Bomb Grab | 50 | 9 s | 25 | 35% on grab + 80% from the bomb 3 s later; glancing hits 15% |
| C | Land Mines (tap: ring / hold: minefield) | 75 | 11 s | 50 | 31.25% per explosion; 8 explosions = 250% at most |
| V | Self Destruct (hold up to 4 s) | 125 | 22.5 s | 100 | 120% to 220%; radius 4 to 10 |
| F | Explosive Jump | 25 | 6 s | 25 | 60%; radius 3.5 |

## Instinct

| Hits that break Instinct | Hits that can be dodged |
|---|---|
| Direct Blast Toss hits | Edges of Blast Toss |
| Targeted Bomb core (within 1.6 blocks) | Edges of Targeted Bomb |
| Bomb Grab delayed blast (`GRAB_DELAYED_BREAKS_INSTINCT`) | The grab itself |
| All Land Mines | |
| Inner 60% of the Self Destruct and Explosive Jump radius | Outer 40% of those radii |

If Instinct dodges the grab, no bomb is attached.

## Land Mines
- **Ring:** 8 bombs land 5 blocks out and go off 2 ticks apart, each as its own hit.
- **Minefield:**
  - Places 4 to 8 mines (more the longer C is held).
  - Mines arm after 1 s and last 30 s; a player keeps at most 12.
  - A mine goes off when something comes within 1.3 blocks.
  - Each explosion sets off the player's other mines within 4.5 blocks, 4 ticks later.
  - The player's other Bomb explosions also set off their mines.
  - Recasting Land Mines sets off the old mines first.
- **Chain limits:** each mine goes off at most once, and at most 6 detonations happen per server tick.

## Self Destruct
- Charging takes health directly, in a straight line, up to 25% of max health at 4 s. Armor, Aura and Defense don't reduce it.
- Health never drops below 2 (`SELF_MIN_HEALTH`, configurable). If there isn't enough health to continue, the bomb detonates immediately.
- You can't move while charging.
- Your head becomes a growing bomb. Your real head and helmet are hidden only while it shows.

## Explosive Jump
- **Launch:** 1.25 blocks/tick straight up when standing still; 1.75 forward and 0.85 up when moving.
- **Repeated-use penalty:**
  - Each use within an 8 s recovery window costs 0, then 6, 12, 20 and 30% of max health (`JUMP_PENALTY`, provisional).
  - The HUD shows the move name in red until you have recovered.
  - The penalty is tracked separately from the normal cooldown.

## Reusable systems
- **Server:**
  - `Traps`: traps and chain reactions.
  - `AttachedCharges`: timed charges stuck to an entity.
  - `HealthCosts` and `HealthCosts.Penalty`: health as a cost, and the repeated-use penalty.
  - `ChargeService.maxTicks`: a charge length per move.
  - `FruitStrikes.Charged`: a held M1 that reports its release.
- **Client:**
  - `ExplosionFx`: layered explosions.
  - The `FIRE` material, using the `blast` GLSL shader.
  - `BombModel`: the bomb and mine models.
  - The `BodyMotion` `BOMB` poses.
  - The bomb-head and helmet-hiding mixins.
  - `FruitStrikeInput`: sends the M1 release.

## Tests
`GrandLineBombGameTests` has 7 tests: identity, Blast Toss, Targeted Bomb, Bomb Grab, Land Mines, Self Destruct, Explosive Jump.
