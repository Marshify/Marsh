#!/usr/bin/env python3
"""Builds the example island data pack (dev/example-datapacks/grandline_example_cove).

A small, working example of everything Grand Line's structure importer reads, in two pieces to show how a big island
is split into several files:

  example_cove.nbt            the islet: grass top, a hut with a Fisherman, a pier with a boat dock, a travel plate,
                              two travel landings and the "origin" marker (pier tip)
  example_cove_lighthouse.nbt a lighthouse rock north of the islet, no origin marker (placed by its corner)

Both files use the real Structure Block format (Minecraft Java 1.20.1, DataVersion 3465), so they can be loaded with a
Structure Block in LOAD mode as well. Run: python3 tools/make_example_island.py
"""
import json, os, sys

sys.path.insert(0, os.path.dirname(__file__))
from make_test_structures import Template  # same writer as the structure test pack

ROOT = os.path.join(os.path.dirname(__file__), '..', 'dev', 'example-datapacks', 'grandline_example_cove')

# World layout: the islet's origin marker (standing space at the pier tip) lands here. Open sea in the First Sea.
ORIGIN_WORLD = (240, 64, -880)
# In the islet template: local y 0 = world y 56 (seabed side), the grass top is local y 7 = world y 63 (one above the
# water surface at y 62), so the pier deck (local y 7) sits just above the water.
BASE_Y = 56


def islet():
    t = Template()
    cx, cz = 7, 7
    for x in range(0, 15):
        for z in range(0, 15):
            d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
            if d <= 6.5:
                t.box(x, 0, z, x, 5, z, 'minecraft:stone')
                t.set(x, 6, z, 'minecraft:dirt')
                t.set(x, 7, z, 'minecraft:grass_block', {'snowy': 'false'})
                t.box(x, 8, z, x, 10, z, 'minecraft:air')   # clear standing room above the grass
            elif d <= 7.5:
                t.box(x, 0, z, x, 6, z, 'minecraft:sand')
    # Hut 5 x 5 (walls 3 high), door gap facing east, Fisherman inside.
    for x in range(3, 8):
        for z in range(3, 8):
            edge = x in (3, 7) or z in (3, 7)
            t.set(x, 7, z, 'minecraft:oak_planks')
            for y in range(8, 11):
                t.set(x, y, z, 'minecraft:spruce_planks' if edge else 'minecraft:air')
            t.set(x, 11, z, 'minecraft:oak_slab', {'type': 'bottom', 'waterlogged': 'false'})
    t.box(7, 8, 5, 7, 9, 5, 'minecraft:air')                  # doorway
    t.set(4, 8, 4, 'minecraft:lantern', {'hanging': 'false', 'waterlogged': 'false'})
    t.marker(5, 8, 5, 'npc:fisherman')
    # Pier: deck east from the shore to x = 20 at z 6..8, fence posts down to the seabed side.
    for x in range(13, 21):
        for z in range(6, 9):
            t.set(x, 7, z, 'minecraft:oak_planks')
            t.box(x, 8, z, x, 10, z, 'minecraft:air')
        if x % 3 == 2:
            for z in (6, 8):
                t.box(x, 0, z, x, 6, z, 'minecraft:oak_fence', {'east': 'false', 'west': 'false', 'north': 'false', 'south': 'false', 'waterlogged': 'true'})
    t.marker(20, 8, 7, 'origin')                             # pier tip, standing space: the placement's "position"
    t.marker(20, 6, 10, 'dock')                              # in the water beside the pier tip: a boat spawns here
    t.marker(17, 8, 7, 'arrival:example_cove_pier')          # landing on the pier
    t.marker(10, 8, 10, 'arrival:example_cove')              # landing on the grass
    t.travel_plate(10, 8, 4, 'example_cove_pier')            # plate on the grass -> pier (landing 6 blocks away)
    return t


def lighthouse():
    t = Template()
    for x in range(0, 7):
        for z in range(0, 7):
            if ((x - 3) ** 2 + (z - 3) ** 2) ** 0.5 <= 3.2:
                t.box(x, 0, z, x, 7, z, 'minecraft:cobblestone')      # rock up to world y 63
    for y in range(8, 18):
        for x in range(2, 5):
            for z in range(2, 5):
                ring = x != 3 or z != 3
                t.set(x, y, z, ('minecraft:white_concrete' if (y // 2) % 2 else 'minecraft:red_concrete') if ring else 'minecraft:air')
    t.set(3, 8, 3, 'minecraft:air')
    t.set(3, 18, 3, 'minecraft:sea_lantern')
    t.box(2, 19, 2, 4, 19, 4, 'minecraft:stone_brick_slab', {'type': 'bottom', 'waterlogged': 'false'})
    return t


def placement(name, template, position, island=''):
    path = os.path.join(ROOT, 'data', 'grandline', 'structure_placements', 'examples', name + '.json')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = {'template': template, 'position': list(position), 'rotation': 'none', 'mirror': 'none'}
    if island:
        data['island'] = island
    json.dump(data, open(path, 'w'), indent=2)


if __name__ == '__main__':
    os.makedirs(ROOT, exist_ok=True)
    json.dump({'pack': {'pack_format': 15, 'description': 'Grand Line example island (Example Cove)'}},
              open(os.path.join(ROOT, 'pack.mcmeta'), 'w'), indent=2)
    sd = os.path.join(ROOT, 'data', 'grandline', 'structures', 'examples')
    islet().save(os.path.join(sd, 'example_cove.nbt'))
    placement('example_cove', 'grandline:examples/example_cove', ORIGIN_WORLD)
    # Second piece: no origin marker, so "position" is its north-west bottom corner. Worked out from the islet:
    # the islet's local (0, 0, 0) is ORIGIN_WORLD - (20, 8, 7); the lighthouse corner sits at islet-local (4, 0, -12).
    islet_corner = (ORIGIN_WORLD[0] - 20, ORIGIN_WORLD[1] - 8, ORIGIN_WORLD[2] - 7)
    lighthouse().save(os.path.join(sd, 'example_cove_lighthouse.nbt'))
    placement('example_cove_lighthouse', 'grandline:examples/example_cove_lighthouse',
              (islet_corner[0] + 4, islet_corner[1], islet_corner[2] - 12))
    print('islet corner', islet_corner, '-> world y', islet_corner[1], '(expected', BASE_Y, ')')
