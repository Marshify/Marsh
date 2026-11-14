# Emberfall Isle rebuild (0.32.0)

A dark volcanic island, Lv 300-375 (FIRST_SEA_PROGRESSION.md). Generated from code in `world/plan/EmberfallIsle.java`; same island id and position. From the south dock: village and crimson trees in front, the volcano in the middle, the military plateau behind it.

## Layout (island-local: +x east, +z south; plain y 66)

| Area | Where | Contents |
|---|---|---|
| Pier and village | south, z 47-98 | 26-block pier on blackstone footings; mayor's house (2 floors, porch, office with desk and shelves, bedroom upstairs), barracks (beds, rack, dummy), storehouse (barrels, chests, shelves), three homes; dark stone plinths, andesite walls between dark-oak posts, iron-bar windows, deepslate-tile roofs (flat with parapets or low pitch), doors open; a square with a brazier |
| Quest giver | (-8, 67, 44) | outside the mayor's house, facing the volcano and the soldiers' ground |
| Soldiers' ground | east, (20, 30) | open blackstone, three grey tents, weapon racks, crates, low barriers (combat area, basic camp) |
| Volcano | centre (0, -18) | base ~84 across, summit ~57 above the plain; radial basalt ribs, magma seams, jagged protrusions; crater (radius 13) with a lava lake, magma rim, five smoking vents; smoke rings, plume and embers drawn by the client |
| Lava | two streams | from springs on the south-west and east slopes, down banked channels (blackstone and magma banks) to rimmed lava pools at the foot; every lava block is a contained source |
| Boss chamber | inside the volcano | tunnel (4 x 5, framed, soul lanterns) from the soldiers' ground; domed cavern (radius 17, 22 high) with a lava lake round a basalt platform (quest bosses), two bridges, basalt columns, magma veins, a lava fall from the crater |
| Military plateau | north, centre (0, -74), y 78 | a broad shelf of three lobes with stepped rocky slopes, a stair ramp up the south-east side, three huts, low walls, target posts, lanterns; spies' ground (combat area, stronger camp) |
| Hidden mine | west foot of the plateau | timber entrance cut into the rock, a framed rail corridor, a drill chamber (iron drill with pistons boring a magma seam, lamps, barrels), a ladder shaft up to a hatch on the plateau |
| Crimson trees | front half | twelve fireproof crimson trees (stems, nether-wart canopies, shroomlight, weeping vines) |

Size: about 150 x 155 of land, the pier to z 98.

## Safety

Lava sits only in the crater, the two banked channels, the foot pools, the chamber lake and the lava fall. Trees are crimson (fireproof), and no wood is near lava. The main route (dock, village, quest giver, soldiers' ground, tunnel, east road, ramp, plateau) never crosses lava; quest bosses stand on the chamber's platform, reached by stone bridges.

## Spots

Quest giver at the mayor's house; Innkeeper by the dock road; Fisherman on the pier; boat; arrival on the landing; camps: soldiers' ground (basic) and the plateau (stronger); quest bosses on the chamber platform; treasure east of the volcano.

## Not done in this pass

The lava-geyser encounter, the mine's searchlight stealth mission, and the awakened volcano boss (five exterior geysers, enhanced attacks) are new gameplay systems; they were not built. The mine is an explorable area without guards or a mission, and no boss beyond the regular quest boss was added.

## Existing worlds

World layout 8. Older worlds keep the original Emberfall (`FirstSeaPlans.EmberfallIsleLegacy`). The smoke rings show only on the rebuilt island.

## Tests

- `GrandLineEmberfallGameTests`: older worlds keep the original; the volcano rises 50-72 above the plain; the plateau is 12 up behind it; village and soldiers' ground are level; soldiers in front, spies behind; quest giver between village and soldiers and clear of both camps; quest bosses in the chamber.
- In game (fresh world): dock view, village, soldiers' ground, volcano with smoke rings, crater, boss chamber with the lava fall, plateau and ramp, mine entrance and drill; quest giver, innkeeper and fisherman placed; enemies in both combat areas.
- Full suite: 109 / 109 pass.
