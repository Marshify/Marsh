#!/usr/bin/env python3
"""Grand Line VFX asset import pipeline.

Drop artwork into tools/vfx/incoming/<category>/ and run:

    python3 tools/vfx/import_vfx.py            # import, then remove the imported files from incoming/
    python3 tools/vfx/import_vfx.py --check    # validate only, change nothing

Each asset is either
  * a PNG                       incoming/<category>/<name>.png
  * a frame sequence folder     incoming/<category>/<name>/0001.png, 0002.png ... (packed into one sprite sheet)
  * a pre-made sprite sheet     a PNG plus "frames": [columns, rows] in its sidecar
  * an OBJ mesh                 incoming/<category>/<name>.obj (+ optional <name>.png texture)
with a sidecar  incoming/<category>/<name>.json  (or one incoming/<category>/_defaults.json for the whole folder):

    {
      "license": "CC0 / your own work / licensed from X (redistribution allowed)",   REQUIRED
      "source": "where it came from",
      "kind": "particle | flipbook | explosion | smoke | beam | lightning | slash | shockwave | magic_circle | aura | emissive | distortion | mesh",
      "frames": [4, 1], "frame_time": 2, "blend": "additive | translucent",
      "primary": "#FFFFFF", "secondary": "#88CCFF",
      "effect": { ... optional full effect definition to write instead of the generated starter ... }
    }

What it does:
  * checks RGBA transparency (an opaque background would show as a rectangle), size (<= 2048) and power-of-two shape;
  * bleeds edge colours into fully transparent pixels so filtered textures get no dark halos;
  * packs numbered frames into a grid sheet and records frames/columns;
  * copies textures to  src/main/resources/assets/grandline/textures/vfx/<category>/<name>.png
    and meshes to       src/main/resources/assets/grandline/vfx/meshes/<category>/<name>.obj;
  * writes a starter multi-layer effect  assets/grandline/vfx/effects/imported/<category>/<name>.json
    (id grandline:imported/<category>/<name>) for the kind, ready to preview in /glvfx and to customise;
  * records licence and source in tools/vfx/manifest.json. Assets without a licence are refused.
Needs Pillow. Never runs anything contained in the assets.
"""
import json
import math
import pathlib
import shutil
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
INCOMING = ROOT / "tools/vfx/incoming"
ASSETS = ROOT / "src/main/resources/assets/grandline"
MANIFEST = ROOT / "tools/vfx/manifest.json"
MAX_SIZE = 2048
KINDS = {"particle", "flipbook", "explosion", "smoke", "beam", "lightning", "slash", "shockwave", "magic_circle", "aura", "emissive", "distortion", "mesh"}


def sidecar(folder, name):
    meta = {}
    d = folder / "_defaults.json"
    if d.exists():
        meta.update(json.loads(d.read_text()))
    s = folder / f"{name}.json"
    if s.exists():
        meta.update(json.loads(s.read_text()))
    return meta


def bleed(img):
    """Copy the colour of the nearest visible pixel into fully transparent ones (alpha stays 0): no dark halos."""
    img = img.convert("RGBA")
    w, h = img.size
    px = img.load()
    done = [[px[x, y][3] > 0 for y in range(h)] for x in range(w)]
    frontier = [(x, y) for x in range(w) for y in range(h) if done[x][y]]
    for _ in range(4):  # a few pixels of bleed is enough for bilinear filtering and mipmaps
        nxt = []
        for x, y in frontier:
            r, g, b, _a = px[x, y]
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and not done[nx][ny]:
                    px[nx, ny] = (r, g, b, 0)
                    done[nx][ny] = True
                    nxt.append((nx, ny))
        frontier = nxt
    return img


def pack(frames):
    imgs = [Image.open(f).convert("RGBA") for f in frames]
    fw, fh = imgs[0].size
    n = len(imgs)
    cols = n if n <= 8 else math.ceil(math.sqrt(n))
    rows = math.ceil(n / cols)
    sheet = Image.new("RGBA", (fw * cols, fh * rows), (0, 0, 0, 0))
    for i, im in enumerate(imgs):
        if im.size != (fw, fh):
            raise ValueError(f"frame {frames[i].name} is {im.size}, expected {(fw, fh)}")
        sheet.paste(im, ((i % cols) * fw, (i // cols) * fh))
    return sheet, [cols, rows]


def check(img, kind):
    problems, warnings = [], []
    w, h = img.size
    if max(w, h) > MAX_SIZE:
        problems.append(f"{w}x{h} is larger than {MAX_SIZE}")
    if w & (w - 1) or h & (h - 1):
        warnings.append(f"{w}x{h} is not a power of two (works, but may filter less cleanly)")
    alpha = img.getchannel("A")
    lo, hi = alpha.getextrema()
    if kind not in ("mesh", "distortion") and lo == 255:
        problems.append("no transparency: the background would render as a visible rectangle")
    return problems, warnings


def starter(kind, tex, frames, meta, mesh=None):
    """A multi-layer starter effect for the asset kind (edit it freely afterwards)."""
    p, s = meta.get("primary", "#FFFFFF"), meta.get("secondary", "#88CCFF")
    blend = meta.get("blend", "translucent" if kind in ("smoke",) else "additive")
    base = {"texture": tex, "blend": blend}
    if frames != [1, 1]:
        base["frames"] = frames
        if meta.get("frame_time"):
            base["frame_time"] = meta["frame_time"]
    fade = [[0, "$primary", 1], [0.7, "$primary", 0.8], [1, "$secondary", 0]]
    glow = {"type": "sprite", "texture": "grandline:textures/fx/glow.png", "life": 6, "size": [1.5, 0.3], "color": [[0, "$primary", 0.7], [1, "$primary", 0]]}
    L = {
        "particle": [dict(base, type="particles", count=12, lifetime=[10, 18], speed=[0.05, 0.15], shape="sphere", radius=0.3, drag=0.92, size=[0.5, 0.1], color=fade, life=2)],
        "emissive": [dict(base, type="sprite", life=20, size=[[0, 0.5], [0.3, 1.6], [1, 1.8]], color=fade), glow],
        "flipbook": [dict(base, type="sprite", life=20, size=2, color=[[0, "$primary", 1], [1, "$primary", 0.9]])],
        "explosion": [dict(base, type="sprite", life=18, size=[[0, 1], [0.3, 3.5], [1, 4]], color=[[0, "#FFFFFF", 1], [0.6, "$primary", 0.9], [1, "$secondary", 0]]),
                      {"type": "particles", "texture": "grandline:textures/fx/spark.png", "count": 14, "lifetime": [8, 14], "speed": [0.2, 0.45], "shape": "sphere",
                       "stretch": 2.5, "gravity": 0.02, "drag": 0.88, "size": [0.15, 0.04], "color": fade, "life": 2},
                      {"type": "shake", "degrees": 1}],
        "smoke": [dict(base, type="particles", count=8, rate=0.5, lifetime=[24, 36], speed=[0.02, 0.06], emit_axis="up", shape="disc", radius=0.6,
                       gravity=-0.004, drag=0.94, spin=1, size=[1.0, 2.6], color=[[0, "$primary", 0.6], [1, "$secondary", 0]], life=20)],
        "beam": [dict(base, type="beam", life=20, length=10, size=0.8, scroll=2, color=fade)],
        "lightning": [dict(base, type="beam", life=6, length=10, size=0.7, wobble=0.8, segments=14, scroll=5, color=fade), glow],
        "slash": [dict(base, type="sprite", facing="direction", life=8, size=[[0, 2], [1, 3]], rotation=-30, spin=-12,
                       color=[[0, "$primary", 1], [0.6, "$primary", 0.9], [1, "$secondary", 0]])],
        "shockwave": [dict(base, type="ring", facing="up", life=12, radius=[0.3, 4], width=[1.2, 0.2], color=fade), glow],
        "magic_circle": [dict(base, type="sprite", facing="up", life=60, spin=1.5, size=[[0, 0], [0.15, 4], [0.85, 4], [1, 4.4]],
                              color=[[0, "$primary", 0], [0.15, "$primary", 0.9], [0.85, "$primary", 0.9], [1, "$secondary", 0]])],
        "aura": [dict(base, type="particles", rate=2, lifetime=[10, 16], speed=[0.03, 0.07], emit_axis="up", shape="sphere", radius=0.5, gravity=-0.01,
                      size=[0.7, 0.2], color=fade, life=40)],
        "distortion": [{"type": "distortion", "life": 30, "size": 2.5, "color": [[0, "$primary", 0.8], [1, "$primary", 0]]}],
        "mesh": [{"type": "mesh", "mesh": mesh, "texture": tex, "blend": blend, "facing": "up", "life": 30, "spin": 30,
                  "size": [[0, 0.5], [0.2, 1.5], [1, 1.6]], "color": fade}],
    }
    return {"category": "imported", "description": f"Imported {kind}: {meta.get('source', 'custom artwork')}", "duration": 20, "primary": p, "secondary": s,
            "layers": L[kind]}


def main():
    check_only = "--check" in sys.argv
    manifest = json.loads(MANIFEST.read_text()) if MANIFEST.exists() else {}
    if not INCOMING.exists():
        print("nothing to import (tools/vfx/incoming/ is missing)")
        return 0
    failed = False
    for folder in sorted(p for p in INCOMING.iterdir() if p.is_dir()):
        category = folder.name
        names = sorted({p.stem for p in folder.iterdir() if p.suffix.lower() in (".png", ".obj") or p.is_dir()} - {"_defaults"})
        for name in names:
            meta = sidecar(folder, name)
            png, obj, seq = folder / f"{name}.png", folder / f"{name}.obj", folder / name
            kind = meta.get("kind") or ("mesh" if obj.exists() else "flipbook" if seq.is_dir() else "particle")
            label = f"{category}/{name}"
            if kind not in KINDS:
                print(f"REFUSED {label}: unknown kind '{kind}' (one of {sorted(KINDS)})")
                failed = True
                continue
            if not meta.get("license"):
                print(f"REFUSED {label}: no \"license\" in its sidecar (state the licence or that it is your own work)")
                failed = True
                continue
            try:
                frames = meta.get("frames", [1, 1])
                img = None
                if seq.is_dir():
                    files = sorted(f for f in seq.iterdir() if f.suffix.lower() == ".png")
                    img, frames = pack(files)
                elif png.exists():
                    img = Image.open(png).convert("RGBA")
                if isinstance(frames, int):
                    frames = [frames, 1]
                problems, warnings = check(img, kind) if img is not None else ([], [])
                if kind == "mesh" and not obj.exists():
                    problems.append("kind mesh needs a .obj")
                for w in warnings:
                    print(f"  warning {label}: {w}")
                if problems:
                    print(f"REFUSED {label}: " + "; ".join(problems))
                    failed = True
                    continue
                tex = f"grandline:textures/vfx/{category}/{name}.png" if img is not None else None
                mesh = f"grandline:vfx/meshes/{category}/{name}.obj" if obj.exists() else None
                effect = meta.get("effect") or starter(kind, tex, frames, meta, mesh)
                print(f"{'ok' if check_only else 'imported'} {label}: {kind}, frames {frames} -> grandline:imported/{category}/{name}")
                if check_only:
                    continue
                if img is not None:
                    out = ASSETS / f"textures/vfx/{category}/{name}.png"
                    out.parent.mkdir(parents=True, exist_ok=True)
                    bleed(img).save(out)
                if obj.exists():
                    out = ASSETS / f"vfx/meshes/{category}/{name}.obj"
                    out.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(obj, out)
                eff = ASSETS / f"vfx/effects/imported/{category}/{name}.json"
                eff.parent.mkdir(parents=True, exist_ok=True)
                eff.write_text(json.dumps(effect, indent=2) + "\n")
                manifest[label] = {"kind": kind, "license": meta["license"], "source": meta.get("source", ""), "texture": tex, "mesh": mesh,
                                   "effect": f"grandline:imported/{category}/{name}"}
                for p in (png, obj, folder / f"{name}.json"):
                    if p.exists():
                        p.unlink()
                if seq.is_dir():
                    shutil.rmtree(seq)
            except Exception as e:  # noqa: BLE001 - report and continue with the other assets
                print(f"REFUSED {label}: {e}")
                failed = True
    if not check_only:
        MANIFEST.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
