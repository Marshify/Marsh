#!/usr/bin/env python3
"""Grand Line sound assets: import, check, and write sounds.json.

    python3 tools/sounds/sync_sounds.py            # convert incoming files, rebuild sounds.json, report
    python3 tools/sounds/sync_sounds.py --check    # report only (changes nothing)

1. Drop generated audio (mp3, wav, flac or ogg) into tools/sounds/incoming/, named after the cue's asset path, e.g.
       tools/sounds/incoming/fruits/flame/inferno_nova/impact.mp3
       tools/sounds/incoming/fruits/flame/inferno_nova/impact_2.mp3     (a variant; picked at random in game)
   The script converts each to mono OGG Vorbis 44.1 kHz (Minecraft only positions mono sounds), normalises loudness
   (about -16 LUFS integrated, -1 dBTP peak, so moves sit at a consistent level and do not clip when layered),
   writes it to src/main/resources/assets/grandline/sounds/<same path>.ogg and removes the incoming file.
2. It then writes src/main/resources/assets/grandline/sounds.json listing every .ogg that exists, grouped by cue,
   with an attenuation distance per stage. Cues without a file keep their placeholder in game.
3. It lists the cues in GrandLineSounds.java that still have no file.

Needs ffmpeg for step 1.
"""
import json
import pathlib
import re
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/grandline"
SOUNDS = ASSETS / "sounds"
INCOMING = ROOT / "tools/sounds/incoming"
CUES_JAVA = ROOT / "src/main/java/com/grandline/audio/GrandLineSounds.java"
# Distance (blocks) at which a cue fades to silence, by its stage (the last part of its id).
RANGE = {"charge": 16, "cast": 32, "travel": 24, "impact": 48, "impact_terrain": 32, "aftermath": 28, "fizzle": 16}
BIG = {"fruits.flame.inferno_nova.impact": 64, "fruits.flame.pyre_ring.cast": 40, "fruits.bomb.self_destruct.impact": 72,
       "fruits.bomb.blast_toss.impact_heavy": 48, "fruits.bomb.bomb_grab.impact_delayed": 44, "fruits.bomb.blast_toss.cast_throw": 24}


def cue_ids():
    return re.findall(r'"((?:fruits|weapons|styles|movement|world)\.[a-z0-9_.]+)"', CUES_JAVA.read_text())


def convert():
    if not INCOMING.exists():
        return []
    if shutil.which("ffmpeg") is None:
        sys.exit("ffmpeg not found: install it to convert incoming files")
    done = []
    for src in sorted(INCOMING.rglob("*")):
        if src.suffix.lower() not in (".mp3", ".wav", ".flac", ".ogg"):
            continue
        rel = src.relative_to(INCOMING).with_suffix(".ogg")
        dst = SOUNDS / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        cmd = ["ffmpeg", "-y", "-loglevel", "error", "-i", str(src), "-ac", "1", "-ar", "44100",
               "-af", "loudnorm=I=-16:TP=-1:LRA=11", "-c:a", "libvorbis", "-q:a", "6", str(dst)]
        subprocess.run(cmd, check=True)
        src.unlink()
        done.append(str(rel))
    return done


def channels(ogg):
    data = ogg.read_bytes()[:200]
    i = data.find(b"\x01vorbis")
    return data[i + 11] if i >= 0 else None


def build():
    events = {}
    problems = []
    for f in sorted(SOUNDS.rglob("*.ogg")):
        rel = f.relative_to(SOUNDS).with_suffix("").as_posix()
        if rel.startswith("music/"):
            # Soundtrack (tools/music/prepare.py): stereo, streamed from disk, one file per event.
            events["music." + rel[len("music/"):].replace("/", ".")] = {"sounds": [{"name": "grandline:" + rel, "stream": True}]}
            continue
        cue = re.sub(r"_\d+$", "", rel).replace("/", ".")
        stage = cue.rsplit(".", 1)[-1]
        ch = channels(f)
        if ch != 1:
            problems.append(f"{rel}.ogg: {'not Ogg Vorbis' if ch is None else f'{ch} channels'} (must be mono Vorbis)")
        entry = {"name": "grandline:" + rel, "attenuation_distance": BIG.get(cue, RANGE.get(stage, 24))}
        events.setdefault(cue, {"sounds": []})["sounds"].append(entry)
    return events, problems


def main():
    check = "--check" in sys.argv
    converted = [] if check else convert()
    events, problems = build()
    if not check:
        (ASSETS / "sounds.json").write_text(json.dumps(events, indent=2) + "\n")
    ids = cue_ids()
    missing = [c for c in ids if c not in events]
    music = [c for c in events if c.startswith("music.")]
    unknown = [c for c in events if c not in ids and not c.startswith("music.")]
    for c in converted:
        print("converted", c)
    print(f"{len(ids) - len(missing)}/{len(ids)} cues have audio, {len(music)} soundtrack track(s)" + ("" if check else "; sounds.json written"))
    for c in missing:
        print("  placeholder:", c, "->", "sounds/" + c.replace(".", "/") + ".ogg")
    for c in unknown:
        print("  not a cue in GrandLineSounds.java:", c)
    for p in problems:
        print("  PROBLEM:", p)
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
