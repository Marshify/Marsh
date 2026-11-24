# Spin Fruit (rework, 0.76.0)

The fruit keeps its id (`grandline:spin`), item, texture, Common rarity, ownership, mastery and its place in the dealer, storage and spawning systems. Its dealer price is now **7,500** (a per-fruit override in `FruitService.PRICE_OVERRIDES`).

| Key | Move | Mastery | Cooldown | Energy | Damage (× Rocket's Missile Fist) |
|---|---|---:|---:|---:|---|
| Z | Razor Wind (hold) | 1 | 7.5 s | 15% on press | 45% tapped, 95% held 2.5 s |
| X | Tornado Assault | 50 | 11 s | 25% | 110% for all hits |
| C | Air Slasher | 90 | 14.25 s | 35% | 125% for all blades |
| V | unused | – | – | – | – |
| F | Helicopter Flight | 25 | 7.5 s (on exit) | 50% + 0.65%/tick | none |

Missile Fist = 8 base damage. Every number below is before mastery and Fruit-stat scaling, which go through the usual `AbilityContext.damage`. So defense, Aura, Instinct, kill credit, mastery XP, bounty and honor all apply as for any move.

## Mechanics

**Razor Wind** (new reusable `Activation.HELD`)
- Energy is spent on the press. The server runs the move every tick while Z is held, and ends it on release or at 2.5 s, which it enforces itself.
- A tap still runs 0.6 s.
- The cutting field is 6 deep and 5 wide in front of the caster. It follows the aim, and the caster can move slowly while using it.
- A hit lands every 10 ticks, which matches vanilla's invulnerability window, so i-frames are never removed:
  - The first two hits (the tap) do 22.5% each.
  - The next three do 16.67% each.
- Each hit applies a short stagger, scaled down by knockback resistance and halved on bosses.
- Stepping out of the field escapes it, and it never damages through walls.

**Tornado Assault**
- After a 5-tick windup, the whirl runs for 40 ticks at up to 12 blocks/s. It eases in and out and is steered by the camera's yaw.
- The server simulates the body's path:
  - walls stop it, and it climbs one-block steps;
  - it follows slopes and ledges;
  - it never passes through blocks;
  - at the end, the player is moved to the server's position if the client drifted.
- Fall distance is reset the whole time.
- Enemies are pulled gently inward, only when farther than 1.2 blocks from the center, and scaled by resistance.
- Damage: hits at ticks 0, 10, 20 and 30 for 20% each, then a 30% finisher that lifts enemies (scaled by resistance).

**Air Slasher**
- Five blades leave on alternating arm sweeps at ticks 6, 9, 12, 15 and 18. A two-arm finishing blade follows at tick 27.
- Range is 35 blocks. Collision is swept, so blades stop at terrain and each blade hits once.
- Aim assist uses a 6° cone and requires line of sight.
- Small blades: 15% each with a light stagger. Finisher: 50% with moderate knockback.
- Each blade clears only its own target's invulnerability for its single hit, never globally.

**Helicopter Flight**
- Uses the shared `FlightService` at flying speed 0.032 (about 7 blocks/s; Blast Off is 0.09).
- Drain is 0.65% of max energy per tick against 8%/s regen, about 10 s from a full bar.
- It ends when F is pressed again, energy runs out, the player is in water (checked directly at the feet and body each tick), they die, disconnect, or change dimension. It can't start in water.
- The cooldown starts on exit.

**Cleanup**
- A held move ends on death, disconnect, server stop and dimension change (new hook in `FlightService`).
- Tornado and Air Slasher stop when the caster dies or changes level.
- Client effects end with their caster. Loop sounds stop with their effect, and the flight loop plays its spin-down cue.

## Animation: `client/fx/BodyMotion` (new, reusable)

Continuous rotational states with spin-up and spin-down:
- `ARM_WHIRL`: arms reach forward and whirl faster the longer the move is held.
- `BODY_SPIN`: arms out, the whole model spins, leaning into travel.
- `ROTOR`: arms above the shoulders sweep fast cones, legs hang with a loose sway, the body pitches with speed and banks into turns.
- `SLASH`: torso twist with alternating arm sweeps synced to each blade beat, then a two-arm finishing sweep.

How it works:
- Arms are aimed by direction about the shoulder joint, so hands stay attached and no joint over-bends.
- Whole-body spin and lean happen only in `PlayerRenderer.setupRotations` (new mixin). The camera and the real facing are untouched.
- `BodyMotion.hand()` gives effect emitters the animated hand positions, so trails and rotor arcs follow the arms.

## Rendering

**`client/fx/WindRender` (new, reusable)**
- A GLSL core shader, `assets/grandline/shaders/core/wind.{json,vsh,fsh}`, registered with Fabric's `CoreShaderRegistrationCallback`. It provides:
  - procedural fbm airflow streaks scrolling along each surface;
  - a bright leading edge;
  - per-vertex fresnel edge light, computed from mesh normals and passed in UV2;
  - a noise dissolve with a bright rim.
- Translucent and additive variants are available.
- Fallback: when an Iris shader pack is active (detected through Iris's public API by reflection) or the shader failed to load, the same geometry is drawn with the plain render types and a baked airflow texture (`wind_flow.png`).
- Depth test is on and depth write is off: nothing becomes an opaque surface and nothing draws through walls.

**`client/fx/WindMesh` (new, reusable)**
- A parametric surface tessellator with fresnel and UV scrolling.
- Builders for crescent blades (tapered, bowed in 3D), spiralling vortex bands, rings and pressure walls.
- Segment counts follow the quality preset (level of detail).

**`client/fx/SpinFx`**
- Razor Wind:
  - overlapping crescent blades thrown from the hands;
  - arm-anchored ribbon trails;
  - velocity-streaked wind sparks;
  - hit flashes.
- Tornado Assault:
  - 2 to 4 independently rotating, counter-rotating layers of spiral bands, wider at the top, with gaps so the player stays visible;
  - two spiral ribbons and ground currents;
  - pulsing pressure walls;
  - terrain-colored debris that orbits, is flung out, and falls and bounces client-side;
  - a pressure ring when it ends, with a 14-tick dissolve.
- Air Slasher:
  - 3D crescents with a bright edge and translucent body, slightly stretched with speed;
  - tip ribbon trails and shed fragments.
  - The finisher adds layered shells, a pressure haze, a shockwave with ground dust, and a two-tick impact frame.
- Helicopter Flight:
  - a wind arc trailing each arm's cone and circular hand trails;
  - a lift ring and downwash;
  - a ground dust ring when within 5 blocks of the ground;
  - it spins down when flight ends.

**Performance**
- Effects farther than 80 blocks are not drawn.
- Debris per tornado is capped (90 × detail), and trail lengths, particle counts and mesh segments scale with Low, Medium, High or Ultra.
- Quality never changes gameplay.

**Palette**

| Use | Colors |
|---|---|
| Core | #F1FCFF |
| Edge | #C5E6F5 |
| Airflow | #91BED8 |
| Vortex | #648FA8 |
| Haze | #B8C1C9 |
| Debris | #A49B87, blended with the color of the terrain underneath |

## Sounds

16 cues of its own (ElevenLabs, levelled; see AUDIO.md).

## Verified

**Game tests** (`GrandLineSpinGameTests`, 9 tests, all passing). They cover:
- identity, price and numbers;
- Razor Wind: tap exactly 45%; full hold exactly 95% and stopped by the server at 2.5 s; a target that steps out takes one hit; nothing behind the caster or past 6 blocks;
- Tornado: travel, the 12 blocks/s cap, damage at most 110%, no fall distance; stops at a wall, which stays intact;
- Air Slasher: exactly 125%, and nothing before the first sweep;
- Flight: slower than Blast Off, the 50% take-off cost, water ends it, cooldown on exit;
- mastery locks and low energy.

**In the dev client** (no shader pack), each move was cast, recorded and checked frame by frame:
- the HUD lists the four moves on Z, X, C and F;
- Razor Wind: the arm whirl and blades, in first and third person;
- Tornado Assault: body spin with a stable camera, the funnel, currents and debris; both husks were pulled, damaged and lifted;
- Air Slasher: alternating sweeps, the finisher, and all six blades hitting the husk;
- Helicopter Flight: the rotor pose, wind arcs, climbing, steering and landing with F.

## Not done or not verified

- **Shader packs:** Iris, BSL and Sodium are not installed here, so the fallback path was not seen in game.
- **Distortion:** screen-space distortion and depth-aware soft particles are not implemented. They need the scene's color and depth buffers, which the vanilla pipeline doesn't expose safely. Faint haze layers stand in for them.
- **Lodestone:** not added. It would be a new required library, and no build of it was tested here.
- **Not tested live:**
  - several players using Spin at once;
  - death or dimension change in the middle of a move (only the cleanup code paths exist);
  - water during flight in the live client (only the game test covers it).
- **The new sounds:** you haven't listened to them yet.
