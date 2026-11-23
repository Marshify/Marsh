#!/usr/bin/env python3
"""Prepare Grand Line soundtrack tracks for the game (developer tool).

    python3 tools/music/prepare.py                 # every review/approved track with a raw file
    python3 tools/music/prepare.py island.verdant_wilds
    python3 tools/music/prepare.py --check         # report only

For each track (raw file tools/music/raw/<id>.mp3, or .wav/.flac):
  1. Loop: finds the point in the last 45 s whose sound best matches the opening (spectrum over 3 s; the latest of
     the near-equal best points, usually a bar line), cuts there
     and crossfades the audio after the cut into the opening (equal power, 1.5 s). Played on repeat, the end
     flows straight into the start with no pause or jump.
  2. Level: plain gain to -18 LUFS integrated (a music bed under the game's -16 LUFS sound effects), lowered if
     needed to keep true peaks under -1.5 dBTP. No compression or limiting.
  3. Encode: stereo OGG Vorbis (quality 4, about 128 kbps) at
     src/main/resources/assets/grandline/sounds/music/<id with dots as slashes>.ogg
Then it writes src/main/resources/assets/grandline/music/soundtrack.json (the game's island/region/ocean/boss
assignments) and rebuilds sounds.json through tools/sounds/sync_sounds.py. Needs ffmpeg and numpy.
"""
import json
import pathlib
import re
import subprocess
import sys

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "tools/music/soundtrack.json"
RAW = ROOT / "tools/music/raw"
ASSETS = ROOT / "src/main/resources/assets/grandline"
OUT = ASSETS / "sounds/music"
GAME_DATA = ASSETS / "music/soundtrack.json"
SR = 48000
TARGET_LUFS, CEILING_DBTP = -18.0, -1.5
XFADE_S, MATCH_S, SEARCH_S, TOLERANCE = 1.5, 3.0, 45.0, 0.02
USED = ("review", "approved")


def decode(path):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", str(path), "-f", "f32le", "-ac", "2", "-ar", str(SR), "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).copy()


def trim_silence(a, threshold_db=-50):
    level = np.max(np.abs(a), axis=1)
    loud = np.nonzero(level > 10 ** (threshold_db / 20))[0]
    return a[loud[0]:loud[-1] + 1] if len(loud) else a


def spectra(mono, start, frames, hop=1024, n=4096):
    win = np.hanning(n)
    out = []
    for i in range(frames):
        s = start + i * hop
        seg = mono[s:s + n]
        if len(seg) < n:
            seg = np.pad(seg, (0, n - len(seg)))
        out.append(np.log1p(np.abs(np.fft.rfft(seg * win))[:1024]))
    m = np.array(out)
    return m / (np.linalg.norm(m) + 1e-9)


def find_loop(a):
    """The cut point L (samples): audio from L on sounds most like the opening."""
    mono = a.mean(axis=1)
    hop = 1024
    frames = int(MATCH_S * SR / hop)
    ref = spectra(mono, 0, frames)
    xf = int(XFADE_S * SR)
    lo = max(int(len(a) * 0.5), len(a) - int(SEARCH_S * SR))
    hi = len(a) - max(xf, int(MATCH_S * SR))
    scores = [(cand, float(np.sum(spectra(mono, cand, frames) * ref))) for cand in range(lo, hi, 512)]
    top = max(s for _, s in scores)
    # Several bar lines usually match about equally well: take the latest of them to keep the most music.
    best, best_score = max((c, s) for c, s in scores if s >= top - TOLERANCE)
    # Refine to the sample where the waveforms line up best (±10 ms), so the seam keeps its phase.
    w = int(0.05 * SR)
    seg = mono[:w]
    best_fine, best_corr = best, -1e9
    for d in range(-480, 481, 4):
        c = best + d
        if c < 0 or c + w > len(mono):
            continue
        corr = float(np.dot(mono[c:c + w], seg))
        if corr > best_corr:
            best_fine, best_corr = c, corr
    return best_fine, best_score


def make_loop(a, cut):
    xf = int(XFADE_S * SR)
    t = np.linspace(0, np.pi / 2, xf, dtype=np.float32)[:, None]
    out = a[:cut].copy()
    out[:xf] = a[:xf] * np.sin(t) + a[cut:cut + xf] * np.cos(t)
    return out


def loudness(a):
    proc = subprocess.run(["ffmpeg", "-hide_banner", "-f", "f32le", "-ac", "2", "-ar", str(SR), "-i", "-",
                           "-af", "ebur128=peak=true", "-f", "null", "-"], input=a.tobytes(), capture_output=True)
    text = proc.stderr.decode(errors="replace")
    summary = text[text.rfind("Summary:"):]
    i = float(re.search(r"I:\s+(-?[\d.]+) LUFS", summary).group(1))
    peak = float(re.search(r"Peak:\s+(-?[\d.inf]+) dBFS", summary).group(1))
    return i, peak


def encode(a, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ac", "2", "-ar", str(SR), "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "4", str(dst)], input=a.tobytes(), check=True)


def prepare(track):
    src = next((RAW / f"{track['id']}{ext}" for ext in (".mp3", ".wav", ".flac") if (RAW / f"{track['id']}{ext}").exists()), None)
    if src is None:
        return None
    a = trim_silence(decode(src))
    cut, score = find_loop(a)
    loop = make_loop(a, cut)
    lufs, _ = loudness(loop)
    gain_db = TARGET_LUFS - lufs
    loop = loop * np.float32(10 ** (gain_db / 20))
    _, peak = loudness(loop)
    if peak > CEILING_DBTP:  # lower the whole track rather than squash its peaks
        loop = loop * np.float32(10 ** ((CEILING_DBTP - peak) / 20))
    final_lufs, final_peak = loudness(loop)
    dst = OUT / (track["id"].replace(".", "/") + ".ogg")
    encode(loop, dst)
    return {"src": src.name, "length": len(loop) / SR, "cut": cut / SR, "match": score, "lufs": final_lufs,
            "peak": final_peak, "file": dst.relative_to(ROOT).as_posix(), "kb": dst.stat().st_size // 1024}


def sound_id(track_id):
    return "grandline:music." + track_id


def write_game_data(tracks):
    """The game's assignments: only tracks whose prepared file exists are listed under "tracks"."""
    present = {t["id"] for t in tracks if t["status"] in USED and (OUT / (t["id"].replace(".", "/") + ".ogg")).exists()}
    data = {"_doc": "Generated by tools/music/prepare.py from tools/music/soundtrack.json; do not edit by hand.",
            "tracks": {i: sound_id(i) for i in sorted(present)}, "islands": {}, "regions": [], "ocean": {}, "bosses": {}}
    for t in tracks:
        if "region" in t:
            data["regions"].append(dict(island=t["island"], track=t["id"], **t["region"]))
        elif "island" in t:
            data["islands"][t["island"]] = t["id"]
        elif "ocean" in t:
            data["ocean"][str(t["ocean"])] = t["id"]
        elif "boss" in t:
            data["bosses"].setdefault(str(t["boss"]["sea"]), {})[t["boss"]["tier"]] = t["id"]
        for extra in t.get("also", []):  # more islands sharing this track
            data["islands"][extra] = t["id"]
    GAME_DATA.parent.mkdir(parents=True, exist_ok=True)
    GAME_DATA.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
    return present


def main():
    check = "--check" in sys.argv
    ids = [a for a in sys.argv[1:] if not a.startswith("--")]
    tracks = json.loads(MANIFEST.read_text())["tracks"]
    for t in tracks:
        if ids and t["id"] not in ids:
            continue
        if t["status"] not in USED and t["id"] not in ids:
            continue
        if check:
            print(f"{t['id']:32s} {t['status']:9s} raw: {'yes' if any((RAW / (t['id'] + e)).exists() for e in ('.mp3', '.wav', '.flac')) else 'no'}")
            continue
        r = prepare(t)
        if r is None:
            print(f"{t['id']:32s} no raw file in tools/music/raw/ (kept the prepared file if there is one)")
            continue
        print(f"{t['id']:32s} {r['length']:6.1f} s loop (cut at {r['cut']:.1f} s, match {r['match']:.3f}), "
              f"{r['lufs']:.1f} LUFS, peak {r['peak']:.1f} dBTP, {r['kb']} KB -> {r['file']}")
    if check:
        return
    present = write_game_data(tracks)
    print(f"{len(present)} track(s) in the game: {', '.join(sorted(present)) or 'none'}")
    subprocess.run([sys.executable, str(ROOT / "tools/sounds/sync_sounds.py")], check=False)


if __name__ == "__main__":
    main()
