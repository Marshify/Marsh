# Grand Line combat audio (0.34.0; Flame audio 0.36.0)

## Status

| Part | State |
|---|---|
| Audio framework (cues, client playback, loops, voice limits, volume settings, asset pipeline) | **Done**, tested in the game |
| Flame: all 5 moves wired to 23 staged cues | **Done**, tested in the game |
| Flame's custom audio files | **Done in 0.36.0**: all 23 cues have their own audio and no placeholders remain (details below). |
| Other fruits, styles, swords, guns, races, movement | Still on their previous vanilla sounds (see the migration plan) |

## How it works

1. **Cues.** `audio/GrandLineSounds.java` (common) lists every cue: one stage of one ability. Each cue has:
   - an id, a category (ability, weapon, movement or environment) and a stage (charge, cast, travel, impact or aftermath);
   - whether it loops, and a hearing range;
   - an optional placeholder.

   Each cue is registered as the sound event `grandline:<id>`.
2. **Playback is client-side, from server beats.** The server already sends a timed beat for each step of a move (`FxEvents`: windup, release, impact with its result, charge pulses, flight end), and the visuals are drawn from those beats. Audio now plays from the same beats (`client/audio/GrandLineAudio.java`, called from `client/fx/FlameFx.java`). As a result:
   - Sound and visuals are in sync, and audio follows real game events (no fixed timers).
   - Every nearby player hears it once, at the right position. The server no longer plays these sounds, so nothing is doubled.
   - A miss never plays a hit sound: Cinder Shots impact uses separate cues for an enemy hit, a terrain hit and burning out.
3. **Loops** (charge, travel, flight) follow a moving point (the fireball, the wave front, the flier). They stop when the effect ends or is interrupted, fading over 6 ticks, and can trigger a closing cue at that moment (the wave dying out, the flames going out on landing). The Inferno Nova charge rises in pitch as it charges.
4. **Limits:**
   - The same cue at the same spot in the same tick plays once.
   - At most 6 copies of a cue play at once, and at most 12 loops in total.
   - Sounds beyond a cue's range are skipped.
   - Every play gets ±4% random pitch.
5. **Volume.** Minecraft's Master and Players sliders still apply. On top of them, **Settings → Sound** has Ability, Weapon, Movement and World volumes, each Off, Low, Med or Full.
6. **Hidden effects.** If "others' effects" is turned off in Settings, other players' move sounds are hidden too. They are drawn and heard from the same beats.
7. **Debugging.** Start the game with the environment variable `GRANDLINE_AUDIO_DEBUG=1` and the log shows every cue played and whether it used its own file or a placeholder.

## Adding audio files

```
tools/sounds/incoming/<cue path>.mp3|wav|flac|ogg     ← drop generated files here
python3 tools/sounds/sync_sounds.py                    ← convert, place, rebuild sounds.json, report
src/main/resources/assets/grandline/sounds/<cue path>.ogg   (result: mono OGG Vorbis 44.1 kHz)
src/main/resources/assets/grandline/sounds.json             (generated; do not edit by hand)
```

- **Cue path:** the cue id with dots turned into slashes. For example, `fruits.flame.inferno_nova.impact` becomes `fruits/flame/inferno_nova/impact.mp3`.
- **Variants:** `impact_2`, `impact_3` and so on are added to the same cue and picked at random.
- **What the script does:**
  - converts each file to **mono** OGG Vorbis (Minecraft only plays mono sounds positionally);
  - normalises loudness to about −16 LUFS with a −1 dBTP ceiling, so moves are consistent and don't clip when layered;
  - sets each cue's fade distance by stage;
  - lists the cues still on placeholders.

  `--check` reports without changing anything.
- **When a cue switches over:** as soon as its file is in `sounds.json`, that cue uses it automatically. Nothing else changes.
- **Naming:** `fruits/<fruit>/<move>/<stage>`, `styles/<style>/<move>/<stage>`, `weapons/<weapon>/<move>/<stage>`, `movement/<move>/<stage>`, `world/<thing>/<stage>`.

## Flame cues and generation prompts

These prompts are written for ElevenLabs Sound Effects or a similar tool. Generate 2–3 takes of the impact and cast sounds and keep the best one or two as variants.

Every prompt ends with the same exclusions: **no voices, no music, no recognisable samples, no clipping**.

### Z Cinder Shots: three quick fireballs

| Cue | Stage | Loop | Placeholder now | Prompt |
|---|---|---|---|---|
| `fruits.flame.cinder_shots.charge` | Charge | no | firecharge | Short fantasy fire ignition: a quick whoosh of flame coiling around a fist, a bright crackling flare and a soft rising hiss. Mid-high frequencies, light low end. Tight and fast, about 0.4 s. Original anime action RPG sound effect, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.cinder_shots.cast` | Cast | no | blaze shoot | Punchy fireball launch from a punch: sharp transient snap, a compressed burst of flame, and a fast forward whoosh. Strong mid punch with a short low thump. Snappy, about 0.35 s, for rapid repeats. Original anime action RPG fire attack SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.cinder_shots.travel` | Travel | **yes** | silent | A small fireball rushing through the air: a steady roaring flame whoosh with a light crackle and gentle flutter. Mid-frequency, airy, no strong low end. A seamless 1.0 s loop with no start or end transient. Original fantasy SFX, no voice, no music, no recognisable samples. |
| `fruits.flame.cinder_shots.impact` | Impact (enemy) | no | firecharge | A fireball striking a body: a meaty punch transient, a burst of flame, a short sizzle and crackle tail. Solid low-mid thump with a bright fire layer. Powerful but compact, about 0.6 s. Original anime action RPG hit SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.cinder_shots.impact_terrain` | Impact (terrain) | no | firecharge | A fireball hitting stone and splashing: a dull stony thud, a spattering burst of flame, a short crackle. Less punch than a body hit. About 0.5 s. Original fantasy SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.cinder_shots.fizzle` | Aftermath (missed) | no | silent | A small fireball burning out in the air: a soft dying puff of flame, a quick fading hiss of embers. Quiet and light. About 0.4 s. Original fantasy SFX, no voice, no music, no recognisable samples. |

### X Blaze Wave: a wall of fire rolling forward

| Cue | Stage | Loop | Placeholder now | Prompt |
|---|---|---|---|---|
| `fruits.flame.blaze_wave.charge` | Charge | no | firecharge | Flames running along an arm before a sweeping attack: a rising roaring whoosh, crackles building in intensity, a tense inhale of fire. Mid range with a growing low rumble. About 0.4 s, ending at its peak. Original anime action RPG SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.blaze_wave.cast` | Cast | no | blaze shoot | A wide sweep releasing a wall of fire: a heavy whooshing swing, a big roaring flame burst, a deep low boom underneath. Cinematic and broad. About 0.8 s. Original anime action RPG fire attack SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.blaze_wave.travel` | Travel | **yes** | fire ambient | A wall of flame rolling across the ground: a thick, continuous roaring fire with a low rumble, turbulent licks and dense crackling. Full range with a strong low-mid body. A seamless 1.5 s loop with no start or end transient. Original fantasy SFX, no voice, no music, no recognisable samples. |
| `fruits.flame.blaze_wave.impact` | Impact | no | firecharge | A wall of fire slamming into an enemy: a heavy body-impact thump, a flaring flame burst, a sizzling crackle tail. Strong low punch. About 0.6 s. Original anime action RPG hit SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.blaze_wave.aftermath` | Aftermath | no | silent | A wave of fire dying out: a collapsing roar, a soft whoosh down to hissing embers and scattered crackles. Fading tail. About 1.2 s. Original fantasy SFX, no voice, no music, no recognisable samples. |

### C Pyre Ring: a ring of fire bursting outward

| Cue | Stage | Loop | Placeholder now | Prompt |
|---|---|---|---|---|
| `fruits.flame.pyre_ring.charge` | Charge | no | firecharge | Flames being drawn inward along the ground toward a planted fighter: a reversed suction whoosh of fire, rising crackles, a tightening low hum. Builds to a peak. About 0.45 s. Original anime action RPG SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.pyre_ring.cast` | Cast | no | explosion | A ring of fire erupting outward from the ground: a deep concussive thump, a sharp flame detonation, a wide outward whoosh. Heavy sub and low-mid punch, bright fire on top. Explosive but not a generic explosion. About 0.9 s. Original anime action RPG SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.pyre_ring.travel` | Travel | **yes** | blaze shoot | A low ring of fire racing outward across the ground: a fast continuous rushing flame, a ground-hugging roar with crackling edges. Mid and low-mid. A seamless 1.0 s loop. Original fantasy SFX, no voice, no music, no recognisable samples. |
| `fruits.flame.pyre_ring.aftermath` | Aftermath | no | silent | Embers settling after a fire burst: scattered soft crackles, a gentle fading hiss, a faint warm low tone. Quiet. About 1.5 s. Original fantasy SFX, no voice, no music, no recognisable samples. |

### V Inferno Nova: a charged giant fireball

| Cue | Stage | Loop | Placeholder now | Prompt |
|---|---|---|---|---|
| `fruits.flame.inferno_nova.charge` | Charge | **yes** | fire ambient | A huge mass of fire gathering power above raised hands: a swirling rumbling roar, a low pulsing throb, a steady rising energy hum (the game raises the pitch as it charges). Deep low end. A seamless 2.0 s loop with no start or end transient. Original anime action RPG SFX, no voice, no music, no recognisable samples. |
| `fruits.flame.inferno_nova.cast` | Cast | no | blaze shoot | Hurling a giant fireball with the whole body: a massive whooshing heave, a deep roaring flame release, a strong sub-bass push. Cinematic and heavy. About 1.0 s. Original anime action RPG SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.inferno_nova.travel` | Travel | **yes** | silent | A giant fireball flying overhead: a huge, dense, rumbling roar with a low drone, swirling turbulence and crackling flames. Strong low end. A seamless 2.0 s loop with no start or end transient. Original fantasy SFX, no voice, no music, no recognisable samples. |
| `fruits.flame.inferno_nova.impact` | Impact | no | explosion | A colossal fire explosion: a sharp cracking detonation transient, a huge deep sub-bass boom, a roaring fireball expansion and a pressure-wave whoosh. The most powerful flame sound, cinematic but clean. About 1.8 s. Original anime action RPG SFX, not a generic Minecraft explosion, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.inferno_nova.aftermath` | Aftermath | no | firecharge | The aftermath of a huge fire explosion: a long rolling low rumble, crackling flames, falling ember debris and a fading hiss. Spacious tail. About 3.0 s. Original fantasy SFX, no voice, no music, no recognisable samples. |

### F Ember Wings: flight

| Cue | Stage | Loop | Placeholder now | Prompt |
|---|---|---|---|---|
| `fruits.flame.ember_wings.cast` | Cast | no | blaze shoot | Fiery wings igniting for lift-off: a bright flaring ignition, a strong upward whoosh, a crackling burst. Mid and high with a short low lift. About 0.8 s. Original anime action RPG SFX, no voice, no music, no recognisable samples, no clipping. |
| `fruits.flame.ember_wings.travel` | Travel | **yes** | fire ambient | Steady flight on wings of fire: a smooth continuous flame roar like a jet of fire, soft wind rush and light crackle. Moderate level, sits under other sounds (the game raises the pitch with speed). A seamless 2.0 s loop with no start or end transient. Original fantasy SFX, no voice, no music, no recognisable samples. |
| `fruits.flame.ember_wings.aftermath` | Aftermath | no | silent | Fiery wings going out on landing: a soft downward whoosh, flames sputtering out into a gentle hiss of embers. About 0.8 s. Original fantasy SFX, no voice, no music, no recognisable samples. |

**Flame audio (0.36.0): all 23 files are in the mod.**

How they were made:
- **Generated** with ElevenLabs Sound Effects (`eleven_text_to_sound_v2`): 2 takes per sound, about 1,130 credits in all. The travel and charge loops used the model's seamless-loop option. Pyre Ring's travel is a 3 s slice of a long take, crossfaded so it loops cleanly.
- **Picked** by measurement: attack, low end, tail and level.
- **Layered:**
  - Cinder Shots cast: punch snap + flame burst.
  - Cinder Shots impact: body thump + flame burst.
  - Blaze Wave cast: swing + flame wall.
  - Pyre Ring cast: ground thump + detonation.
  - Inferno Nova cast: release + heave.
  - Inferno Nova impact: detonation + sub boom + roar.
  - Inferno Nova aftermath: rumble + low-passed fire bed.
- **Trimmed** to each move's timing and **levelled** by stage: impacts −14 LUFS, casts −15, charges −19, loops and aftermaths −20, fizzle −23. A −1.5 dB peak limiter keeps every file from clipping.
- **Checked in the game:** every cue played its own file at its beat, and the heaviest moment in the recorded mix peaked at −1.7 dB. The Nova aftermath plays at 70% under the impact to keep that headroom.
- **Approved** by ear by the owner. Pyre Ring's aftermath was regenerated once (the second option was chosen).

## Rocket cues (0.75.0)

All 12 Rocket cues have their own audio:

| Move | Cues |
|---|---|
| Missile Fist | cast, travel (loop), impact |
| Air Strike | cast, travel (the dive whistle), impact |
| Rocket Crash | charge, cast, impact |
| Blast Off | cast, travel (loop), aftermath |

How they were made:
- **Generated** with ElevenLabs Sound Effects (`eleven_text_to_sound_v2`): 10 single takes, about 170 credits.
- **Reused:** Air Strike's impact is the Missile Fist impact at a lower level. Blast Off's cast is the Rocket Crash launch.
- **Converted** to mono OGG.
- **Levelled:** impacts −13/−14 LUFS, casts −15, the dive −18, charge −19, loops and aftermath −20, with a −1.5 dBTP limiter.
- **Checked in game:** played at their beats in the dev client. Not yet approved by ear by the owner.

## Spin cues (0.76.0)

All 16 Spin cues have their own audio:

| Move | Cues |
|---|---|
| Razor Wind | charge (spin-up), travel (cutting-wind loop, pitch rises with intensity), impact, aftermath (spin-down) |
| Tornado Assault | cast, travel (swirl loop, pitch follows speed), impact, aftermath (pressure release) |
| Air Slasher | cast (×5, rising pitch), cast_final, travel (blade loop), impact, impact_final |
| Helicopter Flight | cast (spin-up), travel (rhythmic air loop, pitch follows speed), aftermath (spin-down, played when the loop ends) |

How they were made:
- **Generated** with ElevenLabs (`eleven_text_to_sound_v2`): 16 single takes, about 270 credits. The prompts ask for rotating air, not an engine.
- **Converted** to mono OGG.
- **Levelled:** −13 to −22 LUFS by stage, with a −1.5 dBTP limiter. The loops have 20 ms edge fades.
- **Not yet approved by ear** by the owner.

## Blade cues (0.77.0)

- **19 files** (ElevenLabs, single takes, about 300 credits; levelled −13 to −22 LUFS):
  - deflect, with two variants and random pitch;
  - M1: extend, swing, crossing swing, cut, heavy finisher;
  - Razor Rush: cast, impact;
  - Blade Dance: flurry, hit, final sweep;
  - Blade Barrage: launch, travel whistle loop, impact, final impact;
  - Blade Flight: spin-up, rotor loop, spin-down.
- **Volume control:** Blade Dance hit sounds are limited to one per tick per caster.
- **Not yet approved by ear** by the owner.

## Spring cues (0.78.0)

- **11 files** (ElevenLabs, single takes; levelled with loudnorm):
  - Knock: cast, impact;
  - Spring Snipe: cast, impact;
  - Spring Cannon: cast, impact;
  - Spring Emperor: rebound, with one variant;
  - Spring Leap: travel loop, aftermath (retract).
- **Volume control:** rebounds are limited per caster so Emperor does not stack.
- **Not yet approved by ear** by the owner.

## Bomb cues (0.79.0)

- **20 files** (ElevenLabs, single takes; the jump launch reuses the grab launch take):
  - Blast Toss: ignition, fuse loop, throw, explosion, heavy explosion;
  - Targeted Bomb: tension and rattle loop, vertical detonation;
  - Bomb Grab: launch, grab and attach, delayed detonation;
  - Land Mines: deployment, arming click, trigger;
  - Self Destruct: transformation, charge loop, max-charge ping, blast, aftermath;
  - Explosive Jump: launch, self-damage warning.
- **Volume control:** at most 3 explosion sounds per tick, each later one quieter.
- **Takes are short** (about 1 second each), so the loops repeat quickly.
- **Not yet approved by ear** by the owner.

## Smoke cues (0.80.0)

- **13 files** (ElevenLabs, single takes):
  - Elemental Reflex: dispersal (at most one every 4 ticks);
  - Smoke Slam: rush, impact;
  - Smoke Blast: release, cloud loop, slash;
  - Smoke Liberation: vortex, hit;
  - Smoke Bomber: transformation, flight loop, bomb drop, detonation, reform.
- **Volume control:** one slash or hit sound per tick, and at most one bomb detonation sound every 2 ticks.
- **Not yet approved by ear** by the owner.

## Spike cues (0.81.0)

- **13 files** (ElevenLabs, single takes):
  - Spike Summon: rumble loop, eruption, heavy eruption;
  - Whirlwind: spikes growing, rotation loop (pitch follows the spin), stab;
  - Spiky Ball: transformation, rolling loop (pitch follows the real speed), impact, unfolding;
  - Spike Barrage: tension loop, rupture, final rupture.
- **Volume control:** at most 2 eruption or stab sounds per tick, each later one quieter.
- **Not yet approved by ear** by the owner.

## Migration plan for the rest

The pattern for each ability is the same:
1. Add its cues to `GrandLineSounds`, with the current vanilla sound as the placeholder.
2. Play them from the beats in its client FX handler.
3. Remove the server-side `playSound` calls.
4. Write a prompt for each cue.

| Group | Today | Plan |
|---|---|---|
| 41 template fruits (Rocket, Spin, Blade, Spring, Bomb, Smoke, Spike, Ice, Sand, Dark, Diamond, Eagle, Light, Rubber, Ghost, Magma, Tremor, Spider, Buddha, Love, Creation, Echo, Phoenix, Portal, Volt, Pain, Blizzard, Magnet, Gas, Orbit, Mammoth, T-Rex, Dough, Ink, Toxin, Spirit, Tiger, Yeti, Kitsune, and the rest) | Shared move templates (`FruitMoves`: strike, bolt, burst, dash, vortex, lingering...) with **one vanilla sound per element** (`Element.sound()`), played by the server | Give the templates FX beats, then cues per **element × template stage** (for example `fruits.<fruit>.bolt.cast`), with element-level files that individual fruits can override. This gives every fruit its own identity without one file per move. |
| Fighting styles (`FightingStyles`) | Vanilla attack, sweep, explode and anvil sounds | Cues per style move: whoosh, impact and heavy impact, plus multi-hit variants |
| Swords (katana, cutlass, Saber, trident) | Element-template moves plus vanilla hits | Cues per sword (blade whoosh, metal cut, heavy hit, special); the Saber shares the boss's crimson theme |
| Guns (`GunItem`) | Crossbow or explosion per gun | Fire, travel, impact and reload cues per gun type |
| Race abilities (`RaceAbilities`) | Vanilla roar, riptide, beacon and so on | Cast and transform cues per race |
| Movement | Fruit dashes use their element's sound; **there is no Flash Step in the mod** | Dash cues (short directional whoosh); a Flash Step sound once that ability exists |
| Saber boss and other bosses | Vanilla | Cues per attack, played from the existing `boss_*` beats |

## Files

- **New:**
  - `src/main/java/com/grandline/audio/GrandLineSounds.java`
  - `src/client/java/com/grandline/client/audio/GrandLineAudio.java`
  - `tools/sounds/sync_sounds.py`, `tools/sounds/incoming/`
  - `src/main/resources/assets/grandline/sounds/`, `sounds.json`
  - this document
- **Changed:**
  - `GrandLineMod` (registers the cues)
  - `FlameMoves` (its server sounds removed; gameplay unchanged)
  - `client/fx/FlameFx` (the cues)
  - `client/fx/GrandLineFx` (ticks the audio; finished effects are now marked done, so anything following them sees them end)
  - `ClientSettings` and `SettingsScreen` (the Sound section)
  - `en_us.json`
