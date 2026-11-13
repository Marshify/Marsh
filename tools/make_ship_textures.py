#!/usr/bin/env python3
"""Draws the ship textures (original pixel art) into assets/grandline/textures/entity/ship/.

Hull textures (512 x 1024) are four horizontal bands of material, so every model box can take its UVs from anywhere
inside its band: hull planks (v 0-255), deck planks (256-511), trim paint (512-767), dark mast wood (768-1023).
Sail textures (256 x 128) match a 64 x 48 x 1 sail box: front face at (1, 1), back face at (66, 1).
Run: python3 tools/make_ship_textures.py
"""
import os
import random
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'grandline', 'textures', 'entity', 'ship')


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def planks(img, box, base, horizontal, plank=4, seam=28, rnd=None):
    x0, y0, x1, y1 = box
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            along, across = (x, y) if horizontal else (y, x)
            row = (across - (y0 if horizontal else x0)) // plank
            f = 0.92 + 0.08 * ((row * 7919) % 5) / 4          # plank-to-plank tone
            f *= 0.96 + rnd.random() * 0.08                   # grain noise
            if (across - (y0 if horizontal else x0)) % plank == plank - 1:
                f *= 0.72                                       # gap between planks
            if (along + row * 11) % seam == 0:
                f *= 0.7                                        # butt joints, staggered per row
            px[x, y] = shade(base, f) + (255,)


def hull_texture(name, wood, deck, trim):
    rnd = random.Random(name)
    img = Image.new('RGBA', (512, 1024))
    planks(img, (0, 0, 512, 256), wood, True, rnd=rnd)
    planks(img, (0, 256, 512, 512), deck, False, plank=4, seam=36, rnd=rnd)
    planks(img, (0, 512, 512, 768), trim, True, plank=5, seam=64, rnd=rnd)
    planks(img, (0, 768, 512, 1024), shade(wood, 0.55), False, plank=3, seam=96, rnd=rnd)
    img.save(os.path.join(OUT, name + '.png'))


def sail_base(color, seam_color):
    img = Image.new('RGBA', (256, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(str(color))
    px = img.load()
    for y in range(0, 50):
        for x in range(0, 132):
            f = 0.95 + rnd.random() * 0.06
            px[x, y] = shade(color, f) + (255,)
    for face in (1, 66):
        for sx in range(face + 8, face + 64, 11):                  # vertical cloth seams
            d.line([(sx, 1), (sx, 48)], fill=seam_color + (255,))
        d.rectangle([face, 1, face + 63, 48], outline=shade(seam_color, 0.9) + (255,))
    return img


def on_faces(img, draw_fn):
    for face in (1, 66):
        draw_fn(ImageDraw.Draw(img), face, 1)


def pirate_emblem(d, fx, fy):
    """Original emblem: a round skull with a bandana over two crossed cutlasses."""
    W, B, R = (236, 232, 222, 255), (24, 22, 26, 255), (176, 40, 40, 255)
    cx, cy = fx + 32, fy + 22
    # crossed cutlasses (behind the skull): curved blades, gold hilts
    for sgn in (-1, 1):
        for t in range(0, 30):
            x = cx + sgn * (-14 + t)
            y = cy + 12 - t + (1 if 8 < t < 22 else 0)
            d.point((x, y), fill=W)
            d.point((x, y + 1), fill=shade(W[:3], 0.8) + (255,))
        d.rectangle([cx + sgn * -15 - 1, cy + 11, cx + sgn * -15 + 1, cy + 14], fill=(214, 172, 60, 255))
    # skull
    d.ellipse([cx - 9, cy - 11, cx + 9, cy + 5], fill=W)
    d.rectangle([cx - 5, cy + 3, cx + 5, cy + 8], fill=W)
    d.rectangle([cx - 9, cy - 11, cx + 9, cy - 7], fill=R)          # bandana
    d.polygon([(cx + 9, cy - 9), (cx + 14, cy - 6), (cx + 12, cy - 3)], fill=R)
    d.ellipse([cx - 6, cy - 5, cx - 2, cy - 1], fill=B)             # eyes
    d.ellipse([cx + 2, cy - 5, cx + 6, cy - 1], fill=B)
    d.polygon([(cx, cy), (cx - 1, cy + 2), (cx + 1, cy + 2)], fill=B)
    for tx in range(cx - 4, cx + 5, 2):
        d.line([(tx, cy + 5), (tx, cy + 8)], fill=B)


def marine_emblem(d, fx, fy):
    """Two navy bands and an anchor inside a ring with a star above it."""
    N, G = (34, 62, 128, 255), (214, 172, 60, 255)
    d.rectangle([fx, fy + 3, fx + 63, fy + 6], fill=N)
    d.rectangle([fx, fy + 41, fx + 63, fy + 44], fill=N)
    cx, cy = fx + 32, fy + 24
    d.ellipse([cx - 13, cy - 13, cx + 13, cy + 13], outline=N, width=2)
    d.rectangle([cx - 1, cy - 9, cx + 1, cy + 9], fill=N)             # shank
    d.ellipse([cx - 3, cy - 12, cx + 3, cy - 7], outline=N, width=2)  # ring
    d.rectangle([cx - 6, cy - 6, cx + 6, cy - 5], fill=N)             # stock
    d.arc([cx - 9, cy - 2, cx + 9, cy + 11], 20, 160, fill=N, width=2)
    d.polygon([(cx - 9, cy + 3), (cx - 11, cy + 7), (cx - 7, cy + 6)], fill=N)
    d.polygon([(cx + 9, cy + 3), (cx + 11, cy + 7), (cx + 7, cy + 6)], fill=N)
    d.regular_polygon((cx, fy + 9, 3), 5, fill=G) if hasattr(d, 'regular_polygon') else None


def neutral_patches(d, fx, fy):
    P = (196, 176, 132, 255)
    d.rectangle([fx + 9, fy + 30, fx + 17, fy + 37], fill=P)
    d.rectangle([fx + 44, fy + 10, fx + 50, fy + 15], fill=P)
    d.rectangle([fx + 30, fy + 40, fx + 35, fy + 44], fill=P)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    hull_texture('sloop', (168, 128, 82), (196, 160, 112), (76, 120, 116))
    hull_texture('brig', (118, 78, 46), (176, 138, 92), (150, 44, 40))
    hull_texture('galleon', (78, 52, 34), (158, 120, 78), (200, 160, 64))
    pirate = sail_base((40, 38, 44), (26, 24, 30))
    on_faces(pirate, pirate_emblem)
    pirate.save(os.path.join(OUT, 'sails_pirate.png'))
    marine = sail_base((236, 234, 226), (204, 200, 190))
    on_faces(marine, marine_emblem)
    marine.save(os.path.join(OUT, 'sails_marine.png'))
    neutral = sail_base((226, 214, 178), (196, 184, 150))
    on_faces(neutral, neutral_patches)
    neutral.save(os.path.join(OUT, 'sails_neutral.png'))
    print('wrote', sorted(os.listdir(OUT)))
