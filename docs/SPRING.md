# Spring Fruit (0.78.0)

Price 60,000. No M1, no passive. Damage is relative to Missile Fist (8).

| Key | Move | Mastery | Cooldown | Energy | Damage |
|---|---|---:|---:|---:|---|
| Z | Knock | 1 | 6 s | 15 | 6.4 (80%), every target on the line once |
| X | Spring Snipe | 20 | 9 s | 25 | 8.8 (110%), swept body dash to 25 blocks |
| C | Spring Cannon | 50 | 13.5 s | 35 | 10.4 (130%) core, rim down to 65%; half when airborne |
| V | Spring Emperor | 125 | 19.5 s | 65 | 4 × 3.2 max (160%), 16-block square, 3 s |
| F | Spring Leap | 75 | 6 s | 35 | none; momentum wall rebounds |

## Instinct
Direct hits (on the line/path or in the impact core) break Instinct via `CombatContext.breaking`; peripheral hits can be dodged.

## Leap momentum
Start 1.1, max 2.0 blocks/tick; ×1.18 per wall rebound (min 3 ticks apart; the same surface within 10 ticks gives ×0.9); ×0.975/tick decay after 12 ticks without a wall; gravity 0.07; steering 6°/tick toward the camera; ground bounce up = max(−vy×0.72, 0.45); ends on stuck, water, 160 ticks, fruit loss or death.

## Infrastructure
- `combat/MovementGuards`: scoped fall and fly-into-wall immunity with a 20-tick grace.
- `combat/ability/KineticBody`: swept AABB movement with contact normals and swept entity hits.
- Client: `SpringMesh` (procedural helix tube), `SpringLimbs` (arm and leg springs, third and first person), `BodyMotion` PUNCH_EXTEND and SPRING_MOVE, `SpringFx` (rings, afterimages, foot ribbons, speed lines, crater decal, debris).

## Tests
`GrandLineSpringGameTests` (7): identity, Knock, Snipe, Cannon, Emperor, Leap, water and locks.
