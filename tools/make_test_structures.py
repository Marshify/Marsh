#!/usr/bin/env python3
"""Builds the placeholder structure test data pack (dev/test-datapacks/grandline_structure_test).

Writes real Minecraft 1.20.1 structure files (.nbt, the Structure Block format) plus placement JSON, as a worked
example of what Grand Line expects from supplied builds: DATA-mode structure blocks name the markers.
"""
import json, os, sys
import nbtlib
from nbtlib import Compound, List, Int, String, Byte

DATA_VERSION = 3465  # Minecraft 1.20.1
ROOT = os.path.join(os.path.dirname(__file__), '..', 'dev', 'test-datapacks', 'grandline_structure_test')


class Template:
    def __init__(self):
        self.blocks = {}  # (x,y,z) -> (name, props, nbt)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (name, props or {}, nbt)

    def box(self, x1, y1, z1, x2, y2, z2, name, props=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, name, props)

    def marker(self, x, y, z, name):
        """A Structure Block in DATA mode: Grand Line reads its data string as a named marker."""
        self.set(x, y, z, 'minecraft:structure_block', {'mode': 'data'},
                 Compound({'id': String('minecraft:structure_block'), 'mode': String('DATA'), 'metadata': String(name)}))

    def travel_plate(self, x, y, z, arrival):
        """Stone pressure plate at (x,y,z) flush with the floor, impulse command block (Needs Redstone) under it."""
        cmd = f'grandline travel @a[dx=0,dy=1,dz=0] {arrival}'
        self.set(x, y - 1, z, 'minecraft:command_block', {'facing': 'up', 'conditional': 'false'}, Compound({
            'id': String('minecraft:command_block'), 'Command': String(cmd), 'auto': Byte(0), 'powered': Byte(0),
            'TrackOutput': Byte(0), 'conditionMet': Byte(0), 'UpdateLastExecution': Byte(1), 'CustomName': String('{"text":"Grand Line travel"}')}))
        self.set(x, y, z, 'minecraft:stone_pressure_plate', {'powered': 'false'})

    def save(self, path):
        xs = [p[0] for p in self.blocks]; ys = [p[1] for p in self.blocks]; zs = [p[2] for p in self.blocks]
        mn = (min(xs), min(ys), min(zs))
        size = (max(xs) - mn[0] + 1, max(ys) - mn[1] + 1, max(zs) - mn[2] + 1)
        palette, index, blocks = [], {}, []
        for (x, y, z), (name, props, nbt) in sorted(self.blocks.items()):
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = Compound({'Name': String(name)})
                if props:
                    entry['Properties'] = Compound({k: String(v) for k, v in props.items()})
                palette.append(entry)
            b = Compound({'pos': List[Int]([Int(x - mn[0]), Int(y - mn[1]), Int(z - mn[2])]), 'state': Int(index[key])})
            if nbt is not None:
                b['nbt'] = nbt
            blocks.append(b)
        root = Compound({'DataVersion': Int(DATA_VERSION), 'size': List[Int]([Int(s) for s in size]),
                         'palette': List[Compound](palette), 'blocks': List[Compound](blocks), 'entities': List[Compound]([])})
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbtlib.File(root).save(path, gzipped=True)
        return mn, size


def placement(name, template, position):
    path = os.path.join(ROOT, 'data', 'grandline', 'structure_placements', 'test', name + '.json')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump({'template': template, 'position': list(position), 'rotation': 'none', 'island': 'Dawnreach Harbor'},
              open(path, 'w'), indent=2)


def summit():
    t = Template()
    t.box(0, 0, 0, 10, 0, 10, 'minecraft:smooth_stone')            # floor
    for x, z in ((0, 0), (10, 0), (0, 10), (10, 10)):
        t.box(x, 1, z, x, 4, z, 'minecraft:stone_bricks')            # pillars
    t.box(0, 5, 0, 10, 5, 10, 'minecraft:stone_brick_slab', {'type': 'bottom'})
    # Marked alcove: chiseled frame around the plate, sea lanterns either side.
    t.box(3, 0, 4, 5, 0, 6, 'minecraft:chiseled_stone_bricks')
    t.travel_plate(4, 1, 5, 'upper_sky')
    t.set(3, 1, 4, 'minecraft:sea_lantern'); t.set(3, 1, 6, 'minecraft:sea_lantern')
    t.marker(0, 1, 5, 'origin')
    t.marker(8, 1, 5, 'arrival:summit')   # return landing, 4 blocks clear of the plate
    return t


def upper_sky():
    t = Template()
    t.box(0, 0, 0, 12, 0, 12, 'minecraft:grass_block')
    t.box(1, -2, 1, 11, -1, 11, 'minecraft:stone')
    t.travel_plate(10, 1, 6, 'summit')                                  # return plate
    t.set(10, 1, 5, 'minecraft:lantern'); t.set(10, 1, 7, 'minecraft:lantern')
    t.marker(0, 1, 6, 'origin')
    t.marker(3, 1, 6, 'arrival:upper_sky')                              # landing, 7 blocks from the return plate
    return t


def undersea():
    """World y = local y. Sea level 62 (water top), sea floor 39."""
    t = Template()
    S, G = 'minecraft:stone', 'minecraft:glass'
    # Mooring deck at the waterline, west of the stack, with posts and a lantern.
    t.box(-6, 62, 3, -1, 62, 7, 'minecraft:spruce_planks')
    for x in (-6, -1):
        for z in (3, 7):
            t.box(x, 40, z, x, 61, z, 'minecraft:spruce_log')
            t.set(x, 63, z, 'minecraft:spruce_fence')
    t.set(-1, 64, 3, 'minecraft:lantern')
    t.marker(-8, 62, 5, 'dock')                                        # boat floats here, beside the deck
    # The sea stack: solid rock from the floor to above the water, 8 x 11.
    t.box(0, 40, 0, 7, 66, 10, S)
    # Doorway above the waterline (3 wide, 3 tall) on the west face.
    t.box(0, 63, 4, 0, 65, 6, 'minecraft:air')
    t.marker(0, 63, 5, 'origin')
    # Sealed stairway: 3 wide (z 4..6), one step down per block east, stone shell all round.
    floor = 62
    x = 1
    while floor > 43:
        t.box(x, floor - 1, 3, x, floor + 4, 7, S)                     # shell column
        t.box(x, floor, 4, x, floor + 3, 6, 'minecraft:air')           # walk space with headroom
        t.box(x, floor, 4, x, floor, 6, 'minecraft:stone_brick_stairs', {'facing': 'west', 'half': 'bottom', 'shape': 'straight', 'waterlogged': 'false'})
        if x % 4 == 0:
            t.set(x, floor + 3, 5, 'minecraft:sea_lantern')
        floor -= 1
        x += 1
    # Landing then a short glass observation tunnel at floor 43.
    for tx in range(x, x + 6):
        t.box(tx, 42, 3, tx, 47, 7, G)
        t.box(tx, 42, 3, tx, 42, 7, 'minecraft:prismarine_bricks')
        t.box(tx, 43, 4, tx, 46, 6, 'minecraft:air')
    cx = x + 6
    # Glass chamber 11 x 13, floor 42, interior 43..51, crown 52 (10 below sea level).
    t.box(cx, 42, -1, cx + 10, 52, 11, G)
    t.box(cx, 42, -1, cx + 10, 42, 11, 'minecraft:prismarine_bricks')
    t.box(cx + 1, 43, 0, cx + 9, 51, 10, 'minecraft:air')
    t.box(cx, 43, 4, cx, 46, 6, 'minecraft:air')                       # opening from the tunnel
    for lx, lz in ((cx + 2, 1), (cx + 8, 1), (cx + 2, 9), (cx + 8, 9)):
        t.set(lx, 43, lz, 'minecraft:sea_lantern')
    t.marker(cx + 5, 43, 5, 'npc:quest_giver')
    t.marker(cx + 3, 43, 2, 'arrival:undersea_lobby')
    return t


if __name__ == '__main__':
    os.makedirs(ROOT, exist_ok=True)
    json.dump({'pack': {'pack_format': 15, 'description': 'Grand Line structure placement tests (dev only)'}},
              open(os.path.join(ROOT, 'pack.mcmeta'), 'w'), indent=2)
    sd = os.path.join(ROOT, 'data', 'grandline', 'structures', 'test')
    # Placements: the "position" is where each template's origin marker lands in the world.
    summit().save(os.path.join(sd, 'summit.nbt')); placement('summit', 'grandline:test/summit', (300, 141, 305))
    upper_sky().save(os.path.join(sd, 'upper_sky.nbt')); placement('upper_sky', 'grandline:test/upper_sky', (300, 201, 346))
    mn, size = undersea().save(os.path.join(sd, 'undersea.nbt'))
    # Origin marker sits at local (0,63,5) - min corner; place the stack's doorway at world (500, 63, 305).
    placement('undersea', 'grandline:test/undersea', (500, 63, 305))
    print('undersea min', mn, 'size', size)
