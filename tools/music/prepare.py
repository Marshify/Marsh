#!/usr/bin/env python3
"""Prepare Grand Line soundtrack tracks for the game (developer tool).

    python3 tools/music/prepare.py                 # every review/approved track with a raw file
    python3 tools/music/prepare.py island.verdant_wilds
    python3 tools/music/prepare.py --check         # report only

For each track (raw file tools/music/raw/<id>.mp3, or .wav/.flac):
  1. Loop: library tracks usually play their loop twice and then stop. If the 8 s before the music stops match the
     same 8 s one cycle earlier (score >= 0.95), the track becomes exactly that last cycle, aligned to the sample, with
     a 40 ms join. Otherwise it finds the point in the last 60% whose sound best matches the opening (spectrum over
     3 s; the latest of the near-equal best points, usually a bar line), cuts there
     and crossfades the audio after the cut into the opening (equal power, 1.5 s). Played on repeat, the end
     flows straight into the start with no pause or jump.
  2. Level: plain gain to -18 LUFS integrated (a music bed under the game's -16 LUFS sound effects), lowered if
     needed to keep true peaks under -1.5 dBTP. No compression or limiting.
  3. Encode: stereo OGG Vorbis (quality 4, about 128 kbps) at
     src/main/resources/assets/grandline/sounds/music/<id with dots as slashes>.ogg
  A track marked "loop": "as_is" in the manifest is already a seamless loop file and is only levelled and encoded.
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
XFADE_S, MATCH_S, TOLERANCE, CYCLE_MIN = 1.5, 3.0, 0.02, 0.95
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
    lo = int(len(a) * 0.4)  # the whole second part: library tracks often repeat their loop once, then ring out
    hi = len(a) - max(xf, int(MATCH_S * SR))
    # Every candidate start (512-sample steps) scored at once: the whole track's spectrogram is computed one time.
    step, n = 512, 4096
    win = np.hanning(n)
    starts = np.arange(0, len(mono) - n, step)
    spec = np.empty((len(starts), 1024), dtype=np.float32)
    for i, s0 in enumerate(starts):
        spec[i] = np.log1p(np.abs(np.fft.rfft(mono[s0:s0 + n] * win))[:1024])
    ratio = hop // step
    ref_raw = spec[0:frames * ratio:ratio]  # the opening, at the reference hop
    ref_n = ref_raw / (np.linalg.norm(ref_raw) + 1e-9)
    first, last = lo // step, min(hi // step, len(starts) - frames * ratio)
    count = max(0, last - first)
    dots = np.zeros(count, dtype=np.float64)
    energy = np.zeros(count, dtype=np.float64)
    sq = np.einsum("ij,ij->i", spec, spec)
    for k in range(frames):
        rows = slice(first + k * ratio, first + k * ratio + count)
        dots += spec[rows] @ ref_n[k]
        energy += sq[rows]
    vals = dots / (np.sqrt(energy) + 1e-9)
    scores = [(int((first + i) * step), float(v)) for i, v in enumerate(vals)]
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


def music_end(mono):
    """Where the music itself stops (before any trailing reverb): the sharpest drop after the last block at full level."""
    w = int(0.01 * SR)
    n = len(mono) // w
    lv = 20 * np.log10(np.sqrt(np.mean(mono[:n * w].reshape(n, w) ** 2, axis=1)) + 1e-9)
    loud = np.nonzero(lv > np.median(lv[int(n * 0.5):]) - 6)[0]
    last = int(loud[-1]) if len(loud) else n - 1
    seg = lv[max(0, last - 30):last + 30]
    return (max(0, last - 30) + int(np.argmin(np.diff(seg))) + 1) * w if len(seg) > 1 else len(mono)


def find_cycle(mono):
    """Library tracks play their loop at least twice and then stop: the length P of that repeating cycle, found where
    the 8 s before the end best match the 8 s one cycle earlier. Returns (loop start, loop end, score)."""
    end = music_end(mono) - int(0.05 * SR)  # just before the stop
    step, n = 512, 4096
    win = np.hanning(n)
    starts = np.arange(0, end - n, step)
    spec = np.empty((len(starts), 512), dtype=np.float32)
    for i, s0 in enumerate(starts):
        spec[i] = np.log1p(np.abs(np.fft.rfft(mono[s0:s0 + n] * win))[:512])
    spec /= np.linalg.norm(spec, axis=1, keepdims=True) + 1e-9
    e, w = len(starts), int(8 * SR / step)
    if e - w <= int(20 * SR / step):
        return None
    tail = spec[e - w:e]
    best_p, best = None, -1.0
    for period in range(int(20 * SR / step), e - w):
        score = float(np.mean(np.sum(tail * spec[e - period - w:e - period], axis=1)))
        if score > best:
            best_p, best = period, score
    # Sample-exact period: line up the last 0.5 s before the end with the same moment one cycle earlier.
    period = best_p * step
    ref = mono[end - SR // 2:end]
    best_fine, best_corr = period, -1e18
    for d in range(-step, step + 1):
        q = period + d
        seg = mono[end - q - SR // 2:end - q]
        corr = float(np.dot(seg, ref))
        if corr > best_corr:
            best_fine, best_corr = q, corr
    return end - best_fine, end, best


def make_cycle_loop(a, start, end, fade_s=0.04):
    """One cycle [start, end) with the samples after `end` faded into its first few milliseconds."""
    out = a[start:end].copy()
    x = int(fade_s * SR)
    t = np.linspace(0, np.pi / 2, x, dtype=np.float32)[:, None]
    out[:x] = a[start:start + x] * np.sin(t) + a[end:end + x] * np.cos(t)
    return out


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
    if track.get("loop") == "as_is":  # already a seamless loop file: keep every sample
        loop, cut, score = decode(src), None, 1.0
    else:
        a = trim_silence(decode(src))
        cycle = find_cycle(a.mean(axis=1))
        if cycle is not None and cycle[2] >= CYCLE_MIN:  # the composer's own loop: one full cycle
            start, cut, score = cycle
            loop = make_cycle_loop(a, start, cut)
            track["_method"] = f"cycle {start / SR:.1f}-{cut / SR:.1f} s"
        else:  # no clear cycle: cut where the music best matches the opening and crossfade into it
            cut, score = find_loop(a)
            loop = make_loop(a, cut)
            track["_method"] = "crossfade to opening"
    lufs, _ = loudness(loop)
    gain_db = TARGET_LUFS - lufs
    loop = loop * np.float32(10 ** (gain_db / 20))
    _, peak = loudness(loop)
    if peak > CEILING_DBTP:  # lower the whole track rather than squash its peaks
        loop = loop * np.float32(10 ** ((CEILING_DBTP - peak) / 20))
    final_lufs, final_peak = loudness(loop)
    dst = OUT / (track["id"].replace(".", "/") + ".ogg")
    encode(loop, dst)
    return {"src": src.name, "length": len(loop) / SR, "cut": (cut or len(loop)) / SR, "match": score, "lufs": final_lufs,
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


def write_credits(tracks, present):
    """Credits for every shipped track (licences ask for them wherever the project lists credits)."""
    used = [t for t in tracks if t["id"] in present]
    lines = ["GRAND LINE SOUNDTRACK CREDITS", "", "All music is used under the licence named for each track; the files were",
             "trimmed to a loop, levelled and re-encoded (OGG Vorbis) for the game.", ""]
    md = ["# Soundtrack credits", "", "Generated by `tools/music/prepare.py` from `tools/music/soundtrack.json`. Every track "
          "below ships inside the mod. The files were trimmed to a loop, levelled and re-encoded (OGG Vorbis) for the game.", "",
          "| Plays on | Track | Composer | Licence | Source |", "|---|---|---|---|---|"]
    for t in used:
        src = t.get("source")
        where = t.get("island") or ("Open ocean (sea %s)" % t["ocean"] if "ocean" in t else
                                    "Boss fights (%s)" % t["boss"]["tier"] if "boss" in t else t["id"])
        if "region" in t:
            where += " (upper region)"
        if src:
            lines += [f"{where}:", f"  {src['credit']}", f"  Licence: {src['license']}  Source: {src['url']}", ""]
            md.append(f"| {where} | {src['title']} | {src['composer']} | {src['license']} | [OpenGameArt]({src['url']}) |")
        else:
            lines += [f"{where}:", "  Created in collaboration with ElevenLabs (Eleven Music)", ""]
            md.append(f"| {where} | {t['id']} | ElevenLabs (Eleven Music) | ElevenLabs terms | generated |")
    (GAME_DATA.parent / "CREDITS.txt").write_text("\n".join(lines) + "\n")
    (ROOT / "docs/SOUNDTRACK_CREDITS.md").write_text("\n".join(md) + "\n")


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
        print(f"{t['id']:32s} {r['length']:6.1f} s loop ({t.pop('_method', 'as is')}, match {r['match']:.3f}), "
              f"{r['lufs']:.1f} LUFS, peak {r['peak']:.1f} dBTP, {r['kb']} KB -> {r['file']}")
    if check:
        return
    present = write_game_data(tracks)
    write_credits(tracks, present)
    print(f"{len(present)} track(s) in the game: {', '.join(sorted(present)) or 'none'}")
    subprocess.run([sys.executable, str(ROOT / "tools/sounds/sync_sounds.py")], check=False)


if __name__ == "__main__":
    main()
