#!/usr/bin/env python3
"""Generate Grand Line soundtrack tracks with the ElevenLabs Music API (developer tool; players never need it).

    export ELEVENLABS_API_KEY=...                      # never commit it; it is only read from the environment
    python3 tools/music/generate.py --estimate          # cost of every planned track, nothing is generated
    python3 tools/music/generate.py island.verdant_wilds          # generate (or regenerate) one track
    python3 tools/music/generate.py --status planned --yes       # generate every planned track

Each track's prompt, length and assignment come from tools/music/soundtrack.json. The raw MP3 goes to
tools/music/raw/<id>.mp3 (not committed) and the track's status becomes "review". Listen to it, then run
tools/music/prepare.py to loop, level and encode it for the game; mark it "approved" in the manifest once accepted.
Regenerating a track only replaces its raw file: no Java changes are needed.
"""
import argparse
import json
import os
import pathlib
import sys
import time
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "tools/music/soundtrack.json"
RAW = ROOT / "tools/music/raw"
API = "https://api.elevenlabs.io/v1/music"
# Credits per second of generated music. ElevenLabs quoted 25/s before the Millbrook test (3,750 for 150 s) and
# charged 15/s for it (2,250); estimates show both so the budget is never under-stated.
QUOTED_PER_S, CHARGED_PER_S = 25, 15


def load():
    return json.loads(MANIFEST.read_text())


def save(data):
    MANIFEST.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def estimate(tracks):
    seconds = sum(t["duration_s"] for t in tracks)
    print(f"{len(tracks)} track(s), {seconds} s of music")
    print(f"  estimate: {seconds * CHARGED_PER_S:,} credits at the charged rate ({CHARGED_PER_S}/s), "
          f"{seconds * QUOTED_PER_S:,} at the quoted rate ({QUOTED_PER_S}/s)")
    for t in tracks:
        print(f"  {t['id']:32s} {t['duration_s']:4d} s  {t['status']}")


def generate(track, model, key):
    body = json.dumps({"prompt": track["prompt"], "music_length_ms": track["duration_s"] * 1000,
                       "model_id": model, "force_instrumental": True}).encode()
    req = urllib.request.Request(API + "?output_format=mp3_48000_192", data=body, method="POST",
                                 headers={"xi-api-key": key, "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=900) as resp:
        audio = resp.read()
    RAW.mkdir(parents=True, exist_ok=True)
    out = RAW / f"{track['id']}.mp3"
    out.write_bytes(audio)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("ids", nargs="*", help="track ids to generate (default: by --status)")
    ap.add_argument("--status", default=None, help="generate every track with this status (e.g. planned)")
    ap.add_argument("--estimate", action="store_true", help="print the cost only")
    ap.add_argument("--yes", action="store_true", help="needed to generate more than one track at once")
    args = ap.parse_args()

    data = load()
    tracks = data["tracks"]
    if args.ids:
        known = {t["id"]: t for t in tracks}
        unknown = [i for i in args.ids if i not in known]
        if unknown:
            sys.exit(f"unknown track id(s): {', '.join(unknown)}")
        chosen = [known[i] for i in args.ids]
    else:
        chosen = [t for t in tracks if t["status"] == (args.status or "planned")]
    estimate(chosen)
    if args.estimate or not chosen:
        return
    if len(chosen) > 1 and not args.yes:
        sys.exit("more than one track: add --yes after checking the estimate")
    key = os.environ.get("ELEVENLABS_API_KEY")
    if not key:
        sys.exit("set ELEVENLABS_API_KEY (it is read from the environment only and never stored)")
    for track in chosen:
        print(f"generating {track['id']} ...", flush=True)
        start = time.time()
        try:
            out = generate(track, data.get("model_id", "music_v2_5"), key)
        except urllib.error.HTTPError as e:
            sys.exit(f"{track['id']}: HTTP {e.code}: {e.read().decode(errors='replace')[:500]}")
        track["status"] = "review"
        track["generated"] = time.strftime("%Y-%m-%d")
        save(data)
        print(f"  {out.relative_to(ROOT)} ({out.stat().st_size // 1024} KB, {time.time() - start:.0f} s)")
    print("next: listen, then python3 tools/music/prepare.py")


if __name__ == "__main__":
    main()
