#!/usr/bin/env python3
"""Draws Grand Line's move-effect textures (original, procedural) into assets/grandline/textures/fx/.

All are white/gray with alpha so the renderer tints them per move (fire orange, ice cyan, lightning yellow...).
  glow.png    64x64   soft radial glow (cores, flashes)
  flame.png   256x64  4 flickering flame tongues (shells, sheets)
  band.png    64x32   vertical band: bright foot fading upward (rings, wave sheets)
  streak.png  16x64   ribbon: bright centre line, soft edges, noisy (trails)
  spark.png   16x16   hard little star (sparks)
Run: python3 tools/make_fx_textures.py
"""
import math, os, random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'grandline', 'textures', 'fx')
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(7)

def save(img, name):
    img.save(os.path.join(OUT, name))

# value noise for organic shapes
def noise_grid(w, h, cell, seed):
    r = random.Random(seed)
    gw, gh = w // cell + 2, h // cell + 2
    g = [[r.random() for _ in range(gw)] for _ in range(gh)]
    def at(x, y):
        fx, fy = x / cell, y / cell
        ix, iy = int(fx), int(fy)
        tx, ty = fx - ix, fy - iy
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[iy][ix % gw] * (1 - tx) + g[iy][(ix + 1) % gw] * tx
        b = g[iy + 1][ix % gw] * (1 - tx) + g[iy + 1][(ix + 1) % gw] * tx
        return a * (1 - ty) + b * ty
    return at

# glow
img = Image.new('RGBA', (64, 64))
for y in range(64):
    for x in range(64):
        d = math.hypot(x - 31.5, y - 31.5) / 31.5
        a = max(0.0, 1 - d) ** 1.8
        c = int(255 * min(1, 0.55 + 0.45 * (1 - d)))
        img.putpixel((x, y), (c, c, c, int(255 * a)))
save(img, 'glow.png')

# flame tongues: 4 frames, each a teardrop shape with noisy edges, hot centre
img = Image.new('RGBA', (256, 64))
for f in range(4):
    n1 = noise_grid(64, 64, 8, 10 + f)
    n2 = noise_grid(64, 64, 4, 20 + f)
    for y in range(64):
        for x in range(64):
            u = (x - 31.5) / 31.5
            v = y / 63.0                      # 0 top (tip) .. 1 bottom (base)
            width = 0.15 + 0.75 * (v ** 0.7)  # narrow tip, wide base
            wob = (n1(x, y) - 0.5) * 0.5 + (n2(x, y) - 0.5) * 0.25
            d = abs(u + wob * (1 - v) * 0.6) / max(0.05, width)
            base_fade = 1 - max(0, (v - 0.82) / 0.18)
            a = max(0.0, 1 - d) * base_fade
            a = min(1.0, a * 1.6)
            if a <= 0.02:
                continue
            heat = max(0, 1 - d * 1.3) * (0.5 + 0.5 * v)
            c = int(150 + 105 * heat)
            img.putpixel((f * 64 + x, y), (c, c, c, int(255 * a)))
save(img, 'flame.png')

# band: bright at the foot, fading to the top, with flame-like ragged top edge
img = Image.new('RGBA', (64, 32))
n = noise_grid(64, 32, 6, 33)
for y in range(32):
    for x in range(64):
        v = 1 - y / 31.0                     # 1 at top .. 0 at foot
        edge = 0.55 + 0.45 * n(x, y)
        a = max(0.0, 1 - v / edge) ** 1.3
        c = int(170 + 85 * (1 - v))
        img.putpixel((x, y), (c, c, c, int(255 * a)))
save(img, 'band.png')

# streak: ribbon across its width (u), length along v
img = Image.new('RGBA', (16, 64))
n = noise_grid(16, 64, 4, 44)
for y in range(64):
    for x in range(16):
        d = abs(x - 7.5) / 7.5
        a = max(0.0, 1 - d) ** 1.4 * (0.75 + 0.25 * n(x, y))
        c = int(180 + 75 * max(0, 1 - d * 1.5))
        img.putpixel((x, y), (c, c, c, int(255 * a)))
save(img, 'streak.png')

# spark: 4-point star
img = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        dx, dy = abs(x - 7.5), abs(y - 7.5)
        a = max(0.0, 1 - min(dx, dy) / 1.2) * max(0.0, 1 - max(dx, dy) / 7.5)
        a = max(a, max(0.0, 1 - math.hypot(dx, dy) / 3.0))
        img.putpixel((x, y), (255, 255, 255, int(255 * min(1, a))))
save(img, 'spark.png')
print('fx textures:', sorted(os.listdir(OUT)))
