#!/usr/bin/env python3
"""Cuts an owner-supplied chart POI image out of its baked white/checker background (border-connected background and
enclosed checker pockets, then the light anti-alias fringe), crops it and scales it to 640 px wide with nearest-neighbour.

    python3 tools/map/cut_poi.py <source.png> <out.png> <preview.png>
"""
import sys
from collections import deque, Counter
from PIL import Image
src, out, prev = sys.argv[1], sys.argv[2], sys.argv[3]
im = Image.open(src).convert('RGBA'); W, H = im.size; px = im.load()
def bg(p):
    r, g, b, _ = p
    return min(r, g, b) >= 232 and max(r, g, b) - min(r, g, b) <= 10
lab = [[0]*W for _ in range(H)]
# components of background-coloured pixels
comps = []
for sy in range(H):
    for sx in range(W):
        if lab[sy][sx] or not bg(px[sx, sy]): continue
        cid = len(comps)+1; q = deque([(sx, sy)]); lab[sy][sx] = cid; pts = []; edge = False
        while q:
            x, y = q.popleft(); pts.append((x, y))
            if x in (0, W-1) or y in (0, H-1): edge = True
            for nx, ny in ((x+1,y),(x-1,y),(x,y+1),(x,y-1)):
                if 0 <= nx < W and 0 <= ny < H and not lab[ny][nx] and bg(px[nx, ny]):
                    lab[ny][nx] = cid; q.append((nx, ny))
        grays = sum(1 for x, y in pts if max(px[x, y][:3]) <= 249)
        comps.append((edge, len(pts), grays, pts))
removed = 0
for edge, n, grays, pts in comps:
    # border-connected background, or an enclosed pocket that carries the checkerboard's grey squares
    if edge or (n >= 150 and grays / n > 0.25):
        for x, y in pts: px[x, y] = (0, 0, 0, 0)
        removed += 1
print('components', len(comps), 'removed', removed, 'enclosed kept', [ (n, round(g/n,2)) for e,n,g,_ in comps if not e and n>=150][:20])
# fringe: light, near-neutral pixels touching transparency are the checkerboard's anti-alias halo
for _ in range(4):
    kill = []
    for y in range(H):
        for x in range(W):
            p = px[x, y]
            if p[3] and min(p[:3]) >= 200 and max(p[:3]) - min(p[:3]) <= 18:
                if any(0 <= x+dx < W and 0 <= y+dy < H and px[x+dx, y+dy][3] == 0
                       for dx in (-1, 0, 1) for dy in (-1, 0, 1) if dx or dy):
                    kill.append((x, y))
    for x, y in kill: px[x, y] = (0, 0, 0, 0)
    print('fringe pass', len(kill))
box = im.getbbox(); im = im.crop(box); print('bbox', box, 'art', im.size)
w = 640; h = round(im.height * w / im.width)
small = im.resize((w, h), Image.NEAREST)
canvas = Image.new('RGBA', (w + 2, h + 2), (0, 0, 0, 0)); canvas.alpha_composite(small, (1, 1))
canvas.save(out); print('out', canvas.size)
bgc = Image.new('RGBA', canvas.size, (30, 60, 110, 255)); bgc.alpha_composite(canvas); bgc.save(prev)
