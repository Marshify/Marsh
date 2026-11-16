"""Lists reachable chest candidates on one island with their context, for choosing spots by hand.

Usage: python3 tools/treasure/query_spots.py <scan.json> [indoor|elevated|dark|crates|any] [limit]
"""
import json, math, statistics, sys
d = json.load(open(sys.argv[1]))
mode = sys.argv[2] if len(sys.argv) > 2 else 'any'
limit = int(sys.argv[3]) if len(sys.argv) > 3 else 40
NPC = {'QUEST', 'ARRIVAL', 'HOME', 'SWORDSMITH', 'FISHERMAN', 'GACHA', 'GUNSMITH', 'NAVIGATOR', 'FRUIT_DEALER', 'TEMPLE_BOSS'}
anchors = d['anchors']
stand = {(c['x'], c['y'], c['z']) for c in d['candidates']} | {tuple(w) for w in d.get('walk', [])}
by_col = {}
for (x, y, z) in stand:
    by_col.setdefault((x, z), []).append(y)
seen, queue = set(), []
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
ground = statistics.median([c[2] for c in d['columns'] if c[2] > 62])
arr = next((a for a in anchors if a['type'] == 'ARRIVAL'), None) or next((a for a in anchors if a['type'] == 'DOCK'), {'x': 0, 'z': 0})
npcs = [a for a in anchors if a['type'] in NPC]
out = []
for c in d['candidates']:
    if (c['x'], c['y'], c['z']) not in seen or c.get('door') or c.get('water'):
        continue
    if any(abs(c['x'] - a['x']) + abs(c['z'] - a['z']) < 6 and abs(c['y'] - a['y']) < 4 for a in npcs):
        continue
    elev = c['y'] - ground
    indoor = c['roof'] > 0 and c['enclosed'] >= 3
    ok = {'indoor': indoor, 'elevated': c['roof'] == 0 and elev >= 4, 'dark': c['sky'] < 8 and c['roof'] > 0,
          'crates': c.get('crates', False), 'any': True}[mode]
    if not ok or not c['walls']:
        continue
    dist = math.hypot(c['x'] - arr['x'], c['z'] - arr['z'])
    out.append((-dist, c, elev))
out.sort(key=lambda t: t[0])
print(d['island'], 'ground', ground, 'reachable', len(seen), 'anchors', [(a['type'][:4], a['x'], a['y'], a['z']) for a in anchors])
shown = []
for negd, c, elev in out:
    if any(abs(c['x'] - s[0]) + abs(c['z'] - s[1]) < 5 for s in shown):
        continue
    shown.append((c['x'], c['z']))
    print(f"{c['x']:5d} {c['y']:4d} {c['z']:5d}  d={-negd:5.1f} elev={elev:4.0f} floor={c['floor']:22s} walls={c['walls']:4s} fronts={c['fronts']:4s} roof={c['roof']:2d} encl={c['enclosed']} sky={c['sky']:2d} {'crates' if c.get('crates') else ''}")
    if len(shown) >= limit:
        break
