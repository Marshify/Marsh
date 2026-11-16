"""Generates the three treasure chest block models (Blockbench-compatible Java block models) and their textures.

Each chest is a set of cuboids (body, an arched lid built from stepped layers, metal bands, corner brackets, the lock
and gems). Every visible face gets its own region of a per-tier texture atlas, painted at 2 pixels per model unit
(32 px per block) by a material painter (planks, metal band, lock plate, gem, skull...), so details such as rivets
and plank seams land exactly where they belong on each face.

Run: python3 tools/treasure/make_chests.py  (writes into src/main/resources/assets/grandline/)
"""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'grandline')
PX = 2          # texture pixels per model unit
ATLAS = 256     # atlas size in pixels

def rgb(h):
    h = h.lstrip('#'); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)

TIERS = {
    'common': dict(
        wood=['#3b2616', '#4a3020', '#55381f', '#5f4128'], seam='#22150b', worn='#6e5034',
        metal=['#5e3c12', '#8d5f22', '#b07a30', '#d8a752'], lock=['#5e3c12', '#9a6a2a', '#c9953e', '#e8bf6a'],
        gem=None, deco=False),
    'rare': dict(
        wood=['#4a1a12', '#5e2218', '#6b2a1c', '#7a3322'], seam='#2a0d08', worn='#8a4230',
        metal=['#4a4f58', '#7d8590', '#a9b1bb', '#dfe5ec'], lock=['#5e3c12', '#a87a30', '#d4a548', '#f0cf7a'],
        gem=['#0d2e8c', '#1f5fe0', '#4fa3ff', '#c8ecff'], deco=True),
    'legendary': dict(
        wood=['#4a0c0c', '#661414', '#781a18', '#8a2420'], seam='#2a0505', worn='#9a3028',
        metal=['#7a4a08', '#c48a16', '#e8b52a', '#ffe680'], lock=['#7a4a08', '#c48a16', '#ecbc34', '#fff09a'],
        gem=['#5a0010', '#b0102a', '#ff3048', '#ffc0c8'], deco=True),
}

# ---------------------------------------------------------------- geometry
# Front of the chest faces north (-z). Units are model units (16 per block).

def box(frm, to, mat, faces='nsewud', skip=''):
    return dict(frm=frm, to=to, mat=mat, faces=[f for f in faces if f not in skip])

def chest_elements(tier):
    big = tier == 'legendary'
    x1, x2 = (0.5, 15.5) if big else (1, 15)
    z1, z2 = (2.5, 13.5) if big else (3, 13)
    top = 9.5 if big else 9                     # body height
    # Arched lid: stepped layers, each narrower front-to-back (the arch shows on the ends).
    steps = [(0, 2.0, 0.0), (2.0, 3.5, 0.5), (3.5, 4.6, 1.5), (4.6, 5.2, 3.0)]
    if big:
        steps = [(0, 2.2, 0.0), (2.2, 3.8, 0.5), (3.8, 5.0, 1.6), (5.0, 5.6, 3.2)]
    els = []
    els.append(box([x1, 0, z1], [x2, top, z2], 'body', skip='d'))
    for i, (a, b, inset) in enumerate(steps):
        els.append(box([x1, top + a, z1 + inset], [x2, top + b, z2 - inset], 'lid', skip='d'))
    bw = 2.4 if big else 2.0                    # band width
    o = 0.3                                     # how far metal stands proud
    band_x = [(x1 + 1.5, x1 + 1.5 + bw), (x2 - 1.5 - bw, x2 - 1.5)]
    if tier != 'common':
        band_x.append((8 - bw / 2, 8 + bw / 2) if tier == 'rare' else (8 - bw / 2 - 0.2, 8 + bw / 2 + 0.2))
    for (a, b) in band_x:
        els.append(box([a, 0, z1 - o], [b, top, z2 + o], 'band', faces='nsewu'))
        for i, (s0, s1, inset) in enumerate(steps):
            els.append(box([a, top + s0, z1 + inset - o], [b, top + s1 + o, z2 - inset + o], 'band', faces='nsewu'))
    # Rim band around the lid seam.
    els.append(box([x1 - 0.2, top - 1.0, z1 - o - 0.1], [x2 + 0.2, top + 0.4, z2 + o + 0.1], 'rim', faces='nsewu'))
    # Bottom rim and corner brackets.
    els.append(box([x1 - 0.2, 0, z1 - o - 0.1], [x2 + 0.2, 1.2, z2 + o + 0.1], 'rim', faces='nsewu'))
    c = 2.4 if big else 2.0
    for (xa, xb) in ((x1 - 0.4, x1 - 0.4 + c), (x2 + 0.4 - c, x2 + 0.4)):
        for (za, zb) in ((z1 - o - 0.25, z1 - o - 0.25 + c), (z2 + o + 0.25 - c, z2 + o + 0.25)):
            els.append(box([xa, 0, za], [xb, c + 0.6, zb], 'corner', faces='nsewu'))
            els.append(box([xa, top - 1.6, za], [xb, top + 0.8, zb], 'corner', faces='nsewu'))
    zf = z1 - o - 0.1                           # front surface of the rim
    if tier == 'legendary':
        # Golden skull lock: cranium, cheek plate and jaw.
        els.append(box([5.4, 4.4, zf - 1.3], [10.6, 10.4, zf], 'skull', faces='nsewu'))
        els.append(box([6.3, 2.6, zf - 1.0], [9.7, 4.6, zf], 'jaw', faces='nsewud'))
        els.append(box([6.9, 10.2, zf - 0.8], [9.1, 12.0, zf + 0.2], 'lock', faces='nsewu'))  # hasp on the lid
    else:
        els.append(box([6.2, 3.4, zf - 0.8], [9.8, 8.6, zf], 'lock', faces='nsewud'))
        els.append(box([6.9, 8.4, zf - 0.6], [9.1, 10.8, zf + 0.2], 'lock', faces='nsewu'))     # hasp
    if TIERS[tier]['gem']:
        lid_top = top + steps[-1][1] + o
        g = 1.6 if big else 1.4
        els.append(box([8 - g, lid_top - 0.2, 8 - g], [8 + g, lid_top + 0.5, 8 + g], 'mount', faces='nsewu'))
        els.append(box([8 - g * 0.6, lid_top + 0.5, 8 - g * 0.6], [8 + g * 0.6, lid_top + 1.4, 8 + g * 0.6], 'gem', faces='nsewu'))
        if tier == 'rare':
            # Gems where the side bands cross the lid seam, and one over the lock.
            for (a, b) in band_x[:2]:
                m = (a + b) / 2
                els.append(box([m - 0.8, top - 1.1, zf - 0.6], [m + 0.8, top + 0.5, zf], 'gem', faces='nsewu'))
            els.append(box([7.0, top + 1.6, z1 - o - 0.5], [9.0, top + 3.4, z1 - o + 0.1], 'gem', faces='nsewud'))
        else:
            els.append(box([7.1, 12.6, z1 - 0.6], [8.9, 14.2, z1 + 0.3], 'gem', faces='nsewud'))
    return els

# ---------------------------------------------------------------- painters
def face_size(el, f):
    (x0, y0, z0), (x1, y1, z1) = el['frm'], el['to']
    w = {'n': x1 - x0, 's': x1 - x0, 'e': z1 - z0, 'w': z1 - z0, 'u': x1 - x0, 'd': x1 - x0}[f]
    h = {'n': y1 - y0, 's': y1 - y0, 'e': y1 - y0, 'w': y1 - y0, 'u': z1 - z0, 'd': z1 - z0}[f]
    return max(1, round(w * PX)), max(1, round(h * PX))

def paint_wood(img, t, w, h, rnd, vertical=False):
    pal = [rgb(c) for c in t['wood']]
    plank = 5
    for y in range(h):
        for x in range(w):
            u, v = (y, x) if vertical else (x, y)
            row = v // plank
            base = pal[(row * 7 + 3) % len(pal)]
            n = rnd.random()
            col = base
            if n < 0.18: col = shade(base, 0.86)
            elif n > 0.93: col = shade(base, 1.12)
            if (u + row * 11) % 9 == 0 and rnd.random() < 0.6: col = shade(base, 0.8)   # grain
            if v % plank == plank - 1: col = rgb(t['seam'])
            if (u * 3 + row * 5) % 23 == 0 and v % plank == 2: col = rgb(t['seam'])      # knots / nail holes
            img.putpixel((x, y), col)
    # weathered highlight along plank tops
    for y in range(h):
        for x in range(w):
            v = x if vertical else y
            if v % plank == 0 and rnd.random() < 0.5:
                img.putpixel((x, y), rgb(t['worn']))

def paint_metal(img, t, w, h, rnd, rivets=True, key='metal'):
    m = [rgb(c) for c in t[key]]
    for y in range(h):
        for x in range(w):
            c = m[2]
            if x == 0 or y == 0: c = m[3]
            if x == w - 1 or y == h - 1: c = m[0]
            elif (x == w - 2 or y == h - 2) and min(w, h) > 3: c = m[1]
            if rnd.random() < 0.08 and 0 < x < w - 1 and 0 < y < h - 1: c = shade(c, 0.9)
            img.putpixel((x, y), c)
    if rivets and min(w, h) >= 3:
        long_h = h >= w
        n = (h if long_h else w)
        cx, cy = w // 2, h // 2
        for i in range(3, n - 2, 6):
            px, py = (cx, i) if long_h else (i, cy)
            if 0 < px < w - 1 and 0 < py < h - 1:
                img.putpixel((px, py), m[3])
                if px + 1 < w - 1: img.putpixel((px + 1, py), m[1])
                if py + 1 < h - 1: img.putpixel((px, py + 1), m[0])

def paint_gem(img, t, w, h, rnd):
    g = [rgb(c) for c in t['gem']]
    for y in range(h):
        for x in range(w):
            d = abs(x - (w - 1) / 2) / max(1, w / 2) + abs(y - (h - 1) / 2) / max(1, h / 2)
            c = g[1] if d < 0.9 else g[0]
            if d < 0.45: c = g[2]
            img.putpixel((x, y), c)
    img.putpixel((max(0, w // 2 - 1), max(0, h // 2 - 1)), g[3])

def paint_lock(img, t, w, h, rnd, front):
    paint_metal(img, t, w, h, rnd, rivets=False, key='lock')
    if front and w >= 5 and h >= 6:
        k = rgb('#1a0f06')
        cx = w // 2
        cy = h // 2 - 1
        for dx in (-1, 0):
            for dy in (-1, 0):
                img.putpixel((cx + dx, cy + dy), k)
        for dy in range(1, 4):
            if cy + dy < h - 1: img.putpixel((cx - 1 if dy < 3 else cx, cy + dy), k); img.putpixel((cx, cy + dy), k)
        m = [rgb(c) for c in t['lock']]
        for (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            img.putpixel((x, y), m[3])

def paint_skull(img, t, w, h, rnd, front):
    paint_metal(img, t, w, h, rnd, rivets=False, key='lock')
    if not front:
        return
    m = [rgb(c) for c in t['lock']]
    dark = rgb('#2a0a04')
    # Cranium shading: highlight top-left, rounded top corners.
    for (x, y) in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1)):
        img.putpixel((x, y), m[0])
    for x in range(2, w - 2):
        img.putpixel((x, 1), m[3])
    # Eye sockets (with a red glint), nose, teeth line.
    ey = h // 2 - 2
    for cx in (w // 2 - 3, w // 2 + 2):
        for dx in (0, 1):
            for dy in (0, 1, 2):
                img.putpixel((cx + dx, ey + dy), dark)
        img.putpixel((cx + (1 if cx < w // 2 else 0), ey + 1), rgb('#c01020'))
    img.putpixel((w // 2 - 1, ey + 4), dark); img.putpixel((w // 2, ey + 4), dark)
    img.putpixel((w // 2 - 1, ey + 5), m[0]); img.putpixel((w // 2, ey + 5), m[0])
    # Keyhole in the forehead.
    img.putpixel((w // 2 - 1, 3), dark); img.putpixel((w // 2, 3), dark); img.putpixel((w // 2, 4), dark)

def paint_jaw(img, t, w, h, rnd, front):
    paint_metal(img, t, w, h, rnd, rivets=False, key='lock')
    if front:
        dark = rgb('#2a0a04')
        for x in range(1, w - 1, 2):
            for y in range(0, max(1, h - 1)):
                img.putpixel((x, y), dark)

def deco_scrolls(img, t, w, h, rnd):
    """Curled metal scrollwork in the corners of the big front and back panels (rare and legendary)."""
    m = [rgb(c) for c in t['metal']]
    if w < 14 or h < 10:
        return
    curl = [(0, 2), (1, 1), (2, 1), (3, 2), (3, 3), (2, 4), (1, 3), (4, 4), (5, 5)]
    for (ox, oy, fx, fy) in ((4, 4, 1, 1), (w - 5, 4, -1, 1), (4, h - 5, 1, -1), (w - 5, h - 5, -1, -1)):
        for (x, y) in curl:
            px, py = ox + x * fx, oy + y * fy
            if 0 <= px < w and 0 <= py < h:
                img.putpixel((px, py), m[3] if (x + y) % 3 == 0 else m[2])

def paint(img, t, mat, f, w, h, rnd, tier):
    if mat in ('body', 'lid'):
        paint_wood(img, t, w, h, rnd, vertical=(mat == 'lid' and f == 'u'))
        if mat == 'body' and f in 'ns' and t['deco']:
            deco_scrolls(img, t, w, h, rnd)
    elif mat in ('band', 'rim', 'mount'):
        paint_metal(img, t, w, h, rnd)
    elif mat == 'corner':
        paint_metal(img, t, w, h, rnd, rivets=False)
        m = [rgb(c) for c in t['metal']]
        if w >= 3 and h >= 3:
            img.putpixel((w // 2, h // 2), m[3]); img.putpixel((w // 2, min(h - 1, h // 2 + 1)), m[0])
    elif mat == 'lock':
        paint_lock(img, t, w, h, rnd, f == 'n')
    elif mat == 'skull':
        paint_skull(img, t, w, h, rnd, f == 'n')
    elif mat == 'jaw':
        paint_jaw(img, t, w, h, rnd, f == 'n')
    elif mat == 'gem':
        paint_gem(img, t, w, h, rnd)

# ---------------------------------------------------------------- packing and output
FACE = {'n': 'north', 's': 'south', 'e': 'east', 'w': 'west', 'u': 'up', 'd': 'down'}

def build(tier):
    t = TIERS[tier]
    rnd = random.Random(tier)
    atlas = Image.new('RGBA', (ATLAS, ATLAS), (0, 0, 0, 0))
    x = y = shelf = 0
    elements = []
    for el in chest_elements(tier):
        faces = {}
        for f in el['faces']:
            w, h = face_size(el, f)
            if x + w > ATLAS:
                x, y, shelf = 0, y + shelf, 0
            if y + h > ATLAS:
                raise SystemExit(f'{tier}: atlas full')
            tile = Image.new('RGBA', (w, h))
            paint(tile, t, el['mat'], f, w, h, rnd, tier)
            atlas.paste(tile, (x, y))
            s = 16 / ATLAS
            faces[FACE[f]] = {'uv': [round(x * s, 4), round(y * s, 4), round((x + w) * s, 4), round((y + h) * s, 4)], 'texture': '#chest'}
            x += w
            shelf = max(shelf, h)
        elements.append({'from': [round(v, 3) for v in el['frm']], 'to': [round(v, 3) for v in el['to']], 'faces': faces})
    name = f'treasure_chest_{tier}'
    os.makedirs(f'{ROOT}/textures/block', exist_ok=True)
    os.makedirs(f'{ROOT}/models/block', exist_ok=True)
    os.makedirs(f'{ROOT}/models/item', exist_ok=True)
    os.makedirs(f'{ROOT}/blockstates', exist_ok=True)
    atlas.save(f'{ROOT}/textures/block/{name}.png')
    model = {'credit': 'Grand Line treasure chest (tools/treasure/make_chests.py)', 'texture_size': [ATLAS, ATLAS],
             'textures': {'chest': f'grandline:block/{name}', 'particle': f'grandline:block/{name}'},
             'elements': elements,
             'display': {
                 'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
                 'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                 'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
                 'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
                 'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}}
    json.dump(model, open(f'{ROOT}/models/block/{name}.json', 'w'), indent=1)
    json.dump({'parent': f'grandline:block/{name}'}, open(f'{ROOT}/models/item/{name}.json', 'w'), indent=1)
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    json.dump({'variants': {f'facing={k}': ({'model': f'grandline:block/{name}', 'y': v} if v else {'model': f'grandline:block/{name}'})
                            for k, v in rot.items()}}, open(f'{ROOT}/blockstates/{name}.json', 'w'), indent=1)
    print(name, len(elements), 'elements')

for tier in TIERS:
    build(tier)
