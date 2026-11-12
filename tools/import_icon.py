#!/usr/bin/env python3
"""Converts an upscaled pixel-art icon into a crisp Minecraft item texture.

Usage: python3 tools/import_icon.py <source.png> <output.png> [--size N]

* Detects the art's pixel grid (each "art pixel" drawn as a big square block) and samples one colour per cell
  (majority vote), so there is no blur or anti-aliasing.
* Removes only the background connected to the image border (near-white or already transparent), so white/cream
  details inside the art (eyes, highlights) are kept.
* Crops to the art, pads to a square power-of-two-friendly canvas, and writes it at native resolution
  (or --size N to force a size, scaled with nearest-neighbour).
"""
import sys
from collections import Counter, deque
from PIL import Image


def is_background(px, threshold=235):
    r, g, b, a = px
    return a < 40 or (r >= threshold and g >= threshold and b >= threshold)


def detect_cell(img):
    """Estimate the art-pixel size from the shortest common run of identical colours along rows and columns."""
    w, h = img.size
    px = img.load()
    runs = Counter()
    for y in range(0, h, max(1, h // 64)):
        run, prev = 1, px[0, y]
        for x in range(1, w):
            cur = px[x, y]
            if max(abs(cur[i] - prev[i]) for i in range(4)) < 24:
                run += 1
            else:
                if run >= 3:
                    runs[run] += 1
                run, prev = 1, cur
    for x in range(0, w, max(1, w // 64)):
        run, prev = 1, px[x, 0]
        for y in range(1, h):
            cur = px[x, y]
            if max(abs(cur[i] - prev[i]) for i in range(4)) < 24:
                run += 1
            else:
                if run >= 3:
                    runs[run] += 1
                run, prev = 1, cur
    if not runs:
        return 1
    # The cell size is the most common small run length (multiples of it are runs of several same-coloured cells).
    common = [length for length, _ in runs.most_common(12)]
    return min(common)


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    source, output = sys.argv[1], sys.argv[2]
    forced = int(sys.argv[sys.argv.index("--size") + 1]) if "--size" in sys.argv else None

    img = Image.open(source).convert("RGBA")
    # Align the grid to the art itself: crop away the plain background margin first.
    mask = Image.new("L", img.size, 0)
    mp, sp = mask.load(), img.load()
    for y in range(img.height):
        for x in range(img.width):
            if not is_background(sp[x, y]):
                mp[x, y] = 255
    img = img.crop(mask.getbbox())
    cell = detect_cell(img)
    cols, rows = round(img.width / cell), round(img.height / cell)
    print(f"source {img.size}, detected art pixel = {cell}px -> grid {cols}x{rows}")

    src = img.load()
    grid = Image.new("RGBA", (cols, rows))
    out = grid.load()
    for gy in range(rows):
        for gx in range(cols):
            votes = Counter()
            x0, y0 = int(gx * img.width / cols), int(gy * img.height / rows)
            x1, y1 = int((gx + 1) * img.width / cols), int((gy + 1) * img.height / rows)
            # Sample the inner part of the cell to avoid edge bleed.
            mx, my = max(1, (x1 - x0) // 4), max(1, (y1 - y0) // 4)
            for y in range(y0 + my, max(y0 + my + 1, y1 - my)):
                for x in range(x0 + mx, max(x0 + mx + 1, x1 - mx)):
                    p = src[min(x, img.width - 1), min(y, img.height - 1)]
                    votes[(p[0] // 8 * 8, p[1] // 8 * 8, p[2] // 8 * 8, 255 if p[3] > 127 else 0)] += 1
            out[gx, gy] = votes.most_common(1)[0][0]

    # Flood-fill the background from the border only.
    seen = set()
    queue = deque((x, y) for x in range(cols) for y in (0, rows - 1)) 
    queue.extend((x, y) for y in range(rows) for x in (0, cols - 1))
    while queue:
        x, y = queue.popleft()
        if (x, y) in seen or not (0 <= x < cols and 0 <= y < rows) or not is_background(out[x, y]):
            continue
        seen.add((x, y))
        out[x, y] = (0, 0, 0, 0)
        queue.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))

    box = grid.getbbox()
    art = grid.crop(box)
    side = max(art.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.alpha_composite(art, ((side - art.width) // 2, side - art.height))  # sit on the bottom like vanilla items
    if forced:
        canvas = canvas.resize((forced, forced), Image.NEAREST)
    canvas.save(output)
    print(f"wrote {output} at {canvas.size[0]}x{canvas.size[1]}")


if __name__ == "__main__":
    main()
