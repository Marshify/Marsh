# Grand Line soundtrack (0.63.0 system, 0.64.0 music)

## Status

| Part | State |
|---|---|
| Music system (island, region, ocean and boss music; crossfades; looping; vanilla music off in Grand Line worlds; Soundtrack volume slider) | **Done**, tested in game |
| Boss fight detection (server) | **Done**, game-tested |
| Tooling (manifest, generator, loop/level/encode preparer, credits) | **Done** |
| Tracks | **Done in 0.64.0: all 19**, open-licence music from OpenGameArt (16 CC-BY 3.0, 3 CC0); credits in [SOUNDTRACK_CREDITS.md](SOUNDTRACK_CREDITS.md) |

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
| `tools/music/soundtrack.json` | The manifest: every track's id, assignment (island, region, ocean or boss tier), status (`planned` → `review` → `approved`), its `source` (title, composer, licence, credit, URL) and an `elevenlabs_prompt` kept in case it is ever generated instead. |
| `tools/music/generate.py` | Calls the ElevenLabs Music API. The key comes from `ELEVENLABS_API_KEY` only and is never stored. |
| `tools/music/prepare.py` | Loops, levels and encodes the raw files, writes the game data file and rebuilds `sounds.json`. |
| `tools/music/raw/` | Raw downloads (not committed). |
| `src/main/resources/assets/grandline/sounds/music/**.ogg` | The game's tracks: stereo OGG Vorbis, about 128 kbps. |
| `src/main/resources/assets/grandline/music/soundtrack.json` | The game's assignments (generated; do not edit by hand). |
| `src/main/resources/assets/grandline/music/CREDITS.txt`, `docs/SOUNDTRACK_CREDITS.md` | Credits for every shipped track (generated from the manifest). |

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

1. **Loop.** Library tracks usually play their loop twice and then stop with a reverb tail. The preparer finds the music's stop and checks whether the 8 s before it match the same 8 s one cycle earlier (score ≥ 0.95).
   - If they match, the track becomes exactly that last cycle, aligned to the sample, with a 40 ms join. It ends on the composer's own loop point.
   - If not, it compares the opening 3 s with the last 60% of the track and cuts at the latest of the near-best matches, usually a bar line. It fine-aligns there (±10 ms) and crossfades the 1.5 s after the cut into the opening.
   - `"loop": "as_is"` keeps a file that is already a seamless loop untouched.
2. **Level.** Plain gain to **-18 LUFS** integrated, a music bed under the -16 LUFS sound effects. The gain is lowered if needed to keep true peaks under -1.5 dBTP. No compression or limiting.
3. **Encode.** Stereo OGG Vorbis, 48 kHz, quality 4.
4. **Write.** The game data file, `sounds.json` and the credits.

## Cost

- **0.64.0 soundtrack:** $0. Every track is free, open-licence music.
- **Spent:** the only money spent was the ElevenLabs Millbrook test (2,250 credits, about $0.45). That track is no longer shipped; its raw file stays in `tools/music/raw/elevenlabs/` (not committed).
- **Size:** about 30 MB of music for 19 tracks.

## Licences

| Licence | What it requires | Tracks |
|---|---|---|
| **CC-BY 3.0** | Credit the composer in the project's credits and say the files were changed. Commercial use is allowed. | Jonathan Shaw's "RPG Orchestral Essentials" (15 tracks), Matthew Pablo, Tausdei |
| **CC0** | Nothing. Credit is appreciated, so they are credited anyway. | PolygonDan, NBJDLukasAbsolute, Indieteur |

- **Jonathan Shaw:** his library's own EULA (bundled with each download) allows commercial and non-commercial use with credit in the credits list. The required form is: "Track" / Composed by Jonathan Shaw / (www.jshaw.co.uk).
- **Matthew Pablo:** the required form is "Music by Matthew Pablo, www.matthewpablo.com".
- **Where the credits appear:** `CREDITS.txt` in the jar, `docs/SOUNDTRACK_CREDITS.md`, the README and the mod's description.
- **Checked:** every licence was read from the track's own OpenGameArt page on 2026-11-23.

## Tracks

| Id | Plays on | Track | Composer | Loop |
|---|---|---|---|---|
| island.millbrook_village | Millbrook Village | In Tranquil Spring | Jonathan Shaw | 75.6 s cycle |
| island.ironhold_bastion | Ironhold Bastion | A Dream of First Flight | Jonathan Shaw | 125.5 s cycle |
| island.dawnreach_harbor | Dawnreach Harbor | Market on the Sea | Jonathan Shaw | 70.0 s cycle |
| island.verdant_wilds | Verdant Wilds | Chill Jungle Ambient | Tausdei | 169.1 s crossfade |
| island.saltmarket_docks | Saltmarket Docks | Merchants and Sea Salt | NBJDLukasAbsolute | 97.7 s crossfade |
| island.sunscorch_dunes | Sunscorch Dunes | Desert Mystic 2 | PolygonDan | 36.0 s crossfade (a short piece) |
| island.frostpeak_isle | Frostpeak Isle | Morning Frost | Jonathan Shaw | 64.0 s cycle |
| island.redbrick_port | Redbrick Port | The Tread of War | Jonathan Shaw | 104.0 s cycle |
| island.cloudtop_isles | Cloudtop Isles | A Tale of Wind | Jonathan Shaw | 106.3 s cycle |
| region.cloudtop_isles_upper | Upper Cloudtop Isles | A Tale of Peace | Jonathan Shaw | 103.7 s cycle |
| island.stonewall_prison | Stonewall Prison | A Bitter Hope | Jonathan Shaw | 68.4 s cycle |
| island.gladiators_ring | Gladiator's Ring | A Fight in the Fields | Jonathan Shaw | 103.2 s cycle |
| island.emberfall_isle | Emberfall Isle | Heated Lands | Jonathan Shaw | 88.6 s cycle |
| island.lotus_palace | Lotus Palace | Underwater World | Indieteur | 80.7 s crossfade |
| island.crystal_fountain | Crystal Fountain | Treading The Unknown | Jonathan Shaw | 90.5 s cycle |
| ocean.first_sea | Open ocean | The World At Your Door | Jonathan Shaw | 74.7 s cycle |
| boss.standard | Quest captains | Showdown of Misdeeds | Jonathan Shaw | 104.0 s cycle |
| boss.major | Temple bosses | A Slave To No One | Jonathan Shaw | 162.7 s cycle |
| boss.final | Crystal Fountain's quest boss | Blackmoor Tides (loop, no chants) | Matthew Pablo | 136.0 s, the composer's loop file |

- **Weak fits:** Gladiator's Ring and Crystal Fountain were the weakest matches when the tracks were chosen. Either can be replaced by dropping a new raw file in and rerunning `prepare.py`, or generated with ElevenLabs (its prompt is kept in the manifest).
