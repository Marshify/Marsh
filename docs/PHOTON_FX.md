# Photon effects for Grand Line

Grand Line uses **Photon 1.1.17 for Fabric 1.20.1** (`photon-fabric-1.20.1-1.1.17.jar`, Modrinth version
`mc1.20.1-1.1.17-fabric`) as an **optional** dependency. The Forge build cannot load in Grand Line, which is a Fabric
mod; the Fabric build is the same Photon version and API.

- With Photon installed and an effect file present, Photon draws that beat.
- Without Photon, or while a file is missing, Grand Line's built-in chunky effects play instead (same timing).
- The server decides *when* each beat happens and sends it to every player within 96 blocks (`FxEventPayload`).
  Photon only changes what each client draws. Damage, hitbox, cooldown and mastery are server-side and unaffected.

## Where files go

`src/main/resources/assets/grandline/fx/<name>.fx` (loaded as `grandline:<name>` by `FXHelper.getFX`).
Photon's editor exports `.fx` files; save/export them with exactly these names.

## Flame X: Burning Blast (first example)

Server timing: windup 6 ticks (0.3 s) → launch (fist travels 1.5 blocks/tick, up to 32 blocks) → impact at the real
collision point. Keep effects inside these durations.

| Beat | File | Attached to | Duration | How Grand Line plays it |
|---|---|---|---|---|
| Windup | `flame_blast_windup.fx` | caster (`EntityEffect`, AutoRotate LOOK), offset `(-0.35, 1.3, 0.2)` = right hand | 8 ticks | on the windup event |
| Fist | `flame_blast_fist.fx` | invisible marker moved along the fist path each tick (`EntityEffect`) | loops until the impact event discards the marker (≤ 24 ticks) | on the launch event |
| Impact | `flame_blast_impact.fx` | world point (`BlockEffect` + offset to the exact hit point) | 12–16 ticks | on the impact event |

### Suggested emitter settings (Photon editor, Particle Emitter)

Chunky style: use square/pixel textures (Photon's default square particle or `minecraft:block/...` textures), no soft
glow textures, and keep counts low so the target stays visible.

**flame_blast_windup.fx**: one emitter
- Duration 8, Looping off, Start lifetime 6–8, Start speed 0, Start size 0.12–0.18
- Shape: Sphere, radius 0.8, emit from shell
- Emission: rate over time 0, burst at 0: 12 particles, burst at 4: 10 particles
- Velocity over lifetime: radial −2.5 (pull towards the centre)
- Color over lifetime: #FFE36A → #FF7A1A → #B3260F, alpha 1 → 0 in the last 20%
- Size over lifetime: 1 → 0.4
- Renderer: Billboard, Additive blend, light 15 (full bright)

**flame_blast_fist.fx**: two emitters
- *Core* (the fist): Duration 1, Looping on, burst 1 particle each tick, lifetime 2, size 0.55, mesh/cube or square
  texture, color #FF8A2A, full bright
- *Trail*: Looping on, rate over distance 6, lifetime 6, start size 0.35, size over lifetime 1 → 0 (tapered),
  color #FFD15A → #FF5A1F → #6B1A0A, simulation space World, inherit velocity 0

**flame_blast_impact.fx**: three emitters
- *Flash*: burst 1 particle, lifetime 2, size 1.2 → 0, color #FFF6D8, additive (small; must not hide the target)
- *Ring*: burst 24, shape Circle (horizontal, radius 0.2, emit from edge), start speed 0.45 outward, lifetime 8,
  size 0.25 → 0.05, color #FFB43A → #E0401A, gravity 0
- *Embers*: burst 12, shape Hemisphere radius 0.3, start speed 0.15–0.35, gravity 0.6, lifetime 10–16,
  size 0.12, color #FFE36A → #FF5A1F

Test in game with `/photon` commands or simply cast Flame X; reload resources (F3+T) after replacing a file.
