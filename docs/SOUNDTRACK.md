# Grand Line soundtrack (0.63.0)

## Status

| Part | State |
|---|---|
| Music system (island, region, ocean and boss music; crossfades; looping; vanilla music off in Grand Line worlds; Soundtrack volume slider) | **Done**, tested in game |
| Boss fight detection (server) | **Done**, game-tested |
| Tooling (manifest, generator, loop/level/encode preparer) | **Done** |
| Tracks | **1 of 19**: Millbrook Village (test track, waiting for approval). The other 18 are written and priced, and are generated only once the test track is approved. |

## How it works in game

- **Choosing the music (client, per player).** Highest priority first:
  1. **Boss fight.** The server tells each player when *they* are fighting a boss (`audio/BossEncounters`). A player joins a fight by hitting a boss or being hit by one. The fight ends when the boss dies, the player dies or moves 64+ blocks away, or 30 s pass without an exchange. Standing near a boss does nothing.
     - Tiers: a quest captain is **standard**; a temple boss (the Saber Warden) is **major**; the quest boss of the last First Sea island (Crystal Fountain) is **final**.
  2. **Special region.** The Upper Cloudtop Isles (Cloudtop Isles at y ≥ 170) have their own track.
  3. **The island you are on.** An island's music area is its plan's built extent. You enter at that distance and leave only 24 blocks further out, so the music never flickers at the edge.
  4. **The open ocean**, by sea.
  - An island whose track isn't in the game yet stays quiet. It doesn't fall back to the ocean music.
- **Changes.**
  - A new choice must hold for 1 s before the music changes, except a boss fight, which starts at once.
  - The change is a 3 s equal-power crossfade.
  - The current track is never restarted. Choosing a track that is fading out brings it back from where it is.
  - Teleports, respawns and dimension changes just choose again. Disconnecting stops everything at once.
- **Playback.**
  - Streamed from disk (`"stream": true`), non-positional and looping.
  - Master category, scaled by **Settings → Sound → Soundtrack** (0–100%, applied immediately).
  - Minecraft's Music slider doesn't affect it.
- **Vanilla music** is silenced in a Grand Line world's overworld (`MusicManagerMixin`). All other sounds are untouched.
- **Resource packs** can replace any track or assignment by overriding `assets/grandline/music/soundtrack.json` and the sound files.

## Files

| File | What |
|---|---|
| `tools/music/soundtrack.json` | The manifest: every track's id, prompt, length, assignment (island, region, ocean or boss tier) and status (`planned` → `review` → `approved`). |
| `tools/music/generate.py` | Calls the ElevenLabs Music API. The key comes from `ELEVENLABS_API_KEY` only and is never stored. |
| `tools/music/prepare.py` | Loops, levels and encodes the raw files, writes the game data file and rebuilds `sounds.json`. |
| `tools/music/raw/` | Raw downloads (not committed). |
| `src/main/resources/assets/grandline/sounds/music/**.ogg` | The game's tracks: stereo OGG Vorbis, about 128 kbps. |
| `src/main/resources/assets/grandline/music/soundtrack.json` | The game's assignments (generated; do not edit by hand). |

## Making or replacing a track (no Java changes)

```
export ELEVENLABS_API_KEY=...                              # never commit it
python3 tools/music/generate.py --estimate                 # cost of every planned track
python3 tools/music/generate.py island.verdant_wilds       # one track (repeat to regenerate it)
python3 tools/music/generate.py --status planned --yes     # every planned track
python3 tools/music/prepare.py                             # loop + level + encode, update the game files
```

- **Your own audio file:** put it at `tools/music/raw/<id>.mp3|wav|flac` and run `prepare.py`.
- **A new island:** add a manifest entry with `"island": "<registry name>"` (the original name; `names.json` only changes what is shown).
- **A new region:** add `"region": {"min_y": …, "max_y": …, "box": [x1, z1, x2, z2]}` (island-relative).
- **Ocean and bosses:** `"ocean": <sea>` gives a sea's ocean track, and `"boss": {"sea": 2, "tier": "major"}` gives a sea's boss track. A sea without its own boss music uses the First Sea's.

### What prepare.py does to each track

1. **Loop.** It compares the spectrum of the opening 3 s with every point in the last 45 s and cuts at the latest of the near-best matches. That is usually a bar line.
   - It fine-aligns the waveform there (±10 ms).
   - It crossfades the 1.5 s after the cut into the opening (equal power).
   - Played on repeat, the end runs straight into the start.
2. **Level.** Plain gain to **-18 LUFS** integrated, a music bed under the -16 LUFS sound effects. The gain is lowered if needed to keep true peaks under -1.5 dBTP. No compression or limiting.
3. **Encode.** Stereo OGG Vorbis, 48 kHz, quality 4.

## Cost

- **Millbrook test:** ElevenLabs quoted 3,750 credits for 150 s (25 credits/s) and charged 2,250 (15 credits/s).
- **Remaining 18 tracks:** 2,670 s, so about **40,050 credits** at the charged rate and up to **66,750** at the quoted rate.
- **Prepared size:** about 1.9 MB per 125 s track, so about 40 MB for all 19.

## Licence

From ElevenLabs' Music Terms (elevenlabs.io/music-terms, checked 2026-11-23):

- **Paid plans:** allow "all online and offline commercial use … except film, TV, radio, and Studio Games".
  - A Studio Game is a monetised game that is not an *Indie Game*.
  - An Indie Game has earned under $500,000 since release, from a publisher earning under $1,000,000 a year from games.
  - A free mod is not monetised and is an Indie Game in any case, so bundling the music in Grand Line is allowed.
- **Free plan:** no commercial licence, and output must credit Eleven Music.
- **Attribution:** some plans require crediting ElevenLabs ("Created in collaboration with ElevenLabs"). The mod's description and the README carry that line on every plan.
- **Plan check:** these rights depend on the plan the music was generated on. Confirm the account generating the tracks is on a paid plan.

## Tracks

| Id | For | Length | Status |
|---|---|---|---|
| island.millbrook_village | Millbrook Village | 150 s (loop 125 s) | review |
| island.ironhold_bastion | Ironhold Bastion | 150 s | planned |
| island.dawnreach_harbor | Dawnreach Harbor | 150 s | planned |
| island.verdant_wilds | Verdant Wilds | 150 s | planned |
| island.saltmarket_docks | Saltmarket Docks | 150 s | planned |
| island.sunscorch_dunes | Sunscorch Dunes | 150 s | planned |
| island.frostpeak_isle | Frostpeak Isle | 150 s | planned |
| island.redbrick_port | Redbrick Port | 150 s | planned |
| island.cloudtop_isles | Cloudtop Isles | 150 s | planned |
| region.cloudtop_isles_upper | Upper Cloudtop Isles | 150 s | planned |
| island.stonewall_prison | Stonewall Prison | 150 s | planned |
| island.gladiators_ring | Gladiator's Ring | 150 s | planned |
| island.emberfall_isle | Emberfall Isle (the volcano island) | 150 s | planned |
| island.lotus_palace | Lotus Palace | 150 s | planned |
| island.crystal_fountain | Crystal Fountain | 150 s | planned |
| ocean.first_sea | Open ocean, First Sea | 180 s | planned |
| boss.standard | Quest captains | 120 s | planned |
| boss.major | Temple bosses | 120 s | planned |
| boss.final | Crystal Fountain's quest boss | 150 s | planned |

The prompts are in the manifest.

**Shared identity:** several prompts ask for a "hopeful rising four-note phrase". A text prompt cannot guarantee the same melody across tracks, so the shared motif is an aim, not a guarantee. Each track is checked by ear before approval.
