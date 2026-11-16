"""Ranks treasure chest spots from /grandline treasure scan output and draws island maps with the picks.

Usage: python3 tools/treasure/pick_spots.py <scan dir> <out dir>
Writes picks.json (per island: tier, x, y, z, facing, why) and <island>.png maps for review. The picks are a
starting point: each one is then looked at in game and adjusted by hand before it goes into TreasureSpots.java.
"""
import json, math, os, sys, statistics
from PIL import Image, ImageDraw

SCAN, OUT = sys.argv[1], sys.argv[2]
NPC = {'QUEST', 'ARRIVAL', 'HOME', 'SWORDSMITH', 'FISHERMAN', 'GACHA', 'GUNSMITH', 'NAVIGATOR', 'FRUIT_DEALER', 'TEMPLE_BOSS'}
PATHS = {'dirt_path', 'gravel', 'sand', 'red_sand', 'water', 'farmland', 'grass_block_path'}
OPP = {'n': 's', 's': 'n', 'e': 'w', 'w': 'e'}
FACING = {'n': 'NORTH', 's': 'SOUTH', 'e': 'EAST', 'w': 'WEST'}
COLORS = {'grass': (90, 150, 60), 'sand': (220, 205, 150), 'stone': (130, 130, 130), 'plank': (160, 120, 70), 'log': (110, 80, 50),
          'leaves': (50, 110, 40), 'water': (50, 90, 180), 'brick': (160, 80, 70), 'snow': (240, 240, 250), 'wool': (200, 60, 60),
          'terracotta': (170, 100, 70), 'dirt': (120, 90, 60), 'glass': (180, 220, 230), 'quartz': (235, 230, 225)}

def colour(b):
    for k, v in COLORS.items():
        if k in b:
            return v
    return (150, 150, 140)

def pick_island(d):
    anchors = d['anchors']
    npcs = [a for a in anchors if a['type'] in NPC]
    arrival = next((a for a in anchors if a['type'] == 'ARRIVAL'), None) or next((a for a in anchors if a['type'] == 'DOCK'), None) \
        or {'x': 0, 'z': 0, 'y': 64}
    tops = {(c[0], c[1]): c[2] for c in d['columns']}
    ground = statistics.median([c[2] for c in d['columns'] if c[2] > 62]) if d['columns'] else 64
    # Reachability: walk from every NPC, dock and arrival over standable blocks (step up 1, drop up to 4).
    stand = {(c['x'], c['y'], c['z']) for c in d['candidates']} | {tuple(w) for w in d.get('walk', [])}
    by_col = {}
    for (x, y, z) in stand:
        by_col.setdefault((x, z), []).append(y)
    seen = set()
    queue = []
    for a in anchors:
        for (x, z) in [(a['x'] + i, a['z'] + j) for i in range(-2, 3) for j in range(-2, 3)]:
            for y in by_col.get((x, z), []):
                if abs(y - a['y']) <= 2 and (x, y, z) not in seen:
                    seen.add((x, y, z)); queue.append((x, y, z))
    while queue:
        x, y, z = queue.pop()
        for (nx, nz) in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
            for ny in by_col.get((nx, nz), []):
                if -4 <= ny - y <= 1 and (nx, ny, nz) not in seen:
                    seen.add((nx, ny, nz)); queue.append((nx, ny, nz))
    cands = []
    for c in d['candidates']:
        if (c['x'], c['y'], c['z']) not in seen:
            continue
        if c.get('door') or c.get('water') or c['floor'] in PATHS or c['floor'].endswith('_leaves'):
            continue
        if any(abs(c['x'] - a['x']) + abs(c['z'] - a['z']) < 6 and abs(c['y'] - a['y']) < 4 for a in npcs):
            continue
        if not c['walls']:
            continue
        faces = [OPP[w] for w in c['walls'] if OPP[w] in c['fronts']]
        if not faces:
            continue
        c['facing'] = faces[0]
        c['dist'] = math.hypot(c['x'] - arrival['x'], c['z'] - arrival['z'])
        c['elev'] = c['y'] - ground
        c['indoor'] = c['roof'] > 0 and c['enclosed'] >= 3
        c['cave'] = c['sky'] < 8 and c['roof'] > 0
        c['outdoor'] = c['roof'] == 0
        cands.append(c)
    def score(c, tier):
        s = 0
        if tier == 'COMMON':
            if not c['outdoor'] or c['elev'] > 3 or (c['enclosed'] < 3 and not c.get('crates')) or len(c['walls']) < 1:
                return None
            s += 4 if c.get('crates') else 0
            s += 2 if len(c['walls']) == 1 else 0
            s += 2 if 12 <= c['dist'] <= 45 else 0
            s -= abs(c['elev']) * 0.5
        elif tier == 'RARE':
            if c['indoor']:
                s += 4
            elif c['outdoor'] and 3 <= c['elev'] <= 14 and len(c['walls']) >= 1:
                s += 3
            else:
                return None
            s += min(c['dist'], 70) / 15
            s += 1 if c.get('crates') else 0
        else:
            if c['cave']:
                s += 6
            elif c['indoor'] and c['enclosed'] == 4:
                s += 3
            elif c['outdoor'] and c['elev'] >= 6:
                s += 3 + min(c['elev'], 30) / 6
            else:
                return None
            s += min(c['dist'], 90) / 12
        return s
    picks = []
    plan = [('LEGENDARY', 1), ('RARE', 1), ('COMMON', 2)]
    if d['island'] == 'Dawnreach Harbor':
        plan = [('RARE', 1), ('COMMON', 2)]
    for tier, n in plan:
        ranked = sorted((c for c in cands if score(c, tier) is not None), key=lambda c: -score(c, tier))
        for c in ranked:
            if sum(1 for p in picks if p['tier'] == tier) >= n:
                break
            if all(math.hypot(c['x'] - p['x'], c['z'] - p['z']) >= 20 for p in picks):
                why = []
                if c.get('crates'): why.append('by crates')
                if c['cave']: why.append('dark, covered')
                elif c['indoor']: why.append('indoors')
                if c['elev'] >= 4: why.append(f"raised {c['elev']:.0f}")
                why.append('walls ' + c['walls'])
                picks.append(dict(tier=tier, x=c['x'], y=c['y'], z=c['z'], facing=FACING[c['facing']], floor=c['floor'], why=', '.join(why)))
    return picks, tops, d['extent'], anchors

os.makedirs(OUT, exist_ok=True)
allp = {}
for f in sorted(os.listdir(SCAN)):
    d = json.load(open(os.path.join(SCAN, f)))
    picks, tops, r, anchors = pick_island(d)
    allp[d['island']] = picks
    S = 4
    img = Image.new('RGB', ((2 * r + 1) * S, (2 * r + 1) * S), (40, 70, 130))
    dr = ImageDraw.Draw(img)
    cols = {(c[0], c[1]): c for c in d['columns']}
    for (x, z), c in cols.items():
        if c[2] <= 62: continue
        col = colour(c[3]); f2 = 0.7 + 0.3 * min(1, max(0, (c[2] - 60) / 40))
        dr.rectangle([(x + r) * S, (z + r) * S, (x + r) * S + S - 1, (z + r) * S + S - 1], fill=tuple(int(v * f2) for v in col))
    for a in anchors:
        dr.ellipse([(a['x'] + r) * S - 4, (a['z'] + r) * S - 4, (a['x'] + r) * S + 4, (a['z'] + r) * S + 4], outline=(255, 255, 255))
    tc = {'COMMON': (255, 255, 255), 'RARE': (60, 160, 255), 'LEGENDARY': (255, 200, 0)}
    for p in picks:
        x, z = (p['x'] + r) * S, (p['z'] + r) * S
        dr.rectangle([x - 6, z - 6, x + 6, z + 6], outline=(0, 0, 0), fill=tc[p['tier']])
    img.save(os.path.join(OUT, f.replace('.json', '.png')))
    print(d['island'], [(p['tier'][0], p['x'], p['y'], p['z'], p['facing'][0], p['why']) for p in picks])
json.dump(allp, open(os.path.join(OUT, 'picks.json'), 'w'), indent=1)
