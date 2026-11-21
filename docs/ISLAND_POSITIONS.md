# First Sea island positions: the three-way correction (layout 19)

Three First Sea islands were standing in each other's places. In layout 19 their structures moved, as a three-way
swap of island centres. Builds, orientations, radii, level requirements, NPCs, treasure spots and names are unchanged.
Everything is placed relative to the island centre, so it all moved with each island.

| Island | Before (layout ≤ 18) | Now (layout 19+) | Radius | Level |
|---|---|---|---|---|
| Redbrick Port (Marines' start) | (-630, 120) | (-1170, 270), Stonewall Prison's old place | 34 | 1 |
| Ironhold Bastion | (1380, 0) | (-630, 120), Redbrick Port's old place | 38 | 120 |
| Stonewall Prison | (-1170, 270) | (1380, 0), Ironhold Bastion's old place | 32 | 190 |

**Where it lives.** `SeaLayout` holds the new centres; `SeaLayout.LEGACY_POSITIONS` records the old ones.
`SeaLayout.all()` returns the positions of the running world's layout (`IslandRegistry.layout()`). Everything that places
or finds islands reads it, so one world always has one consistent set:
- island ids (the centre's chunk) and the island registry
- terrain and structure generation, and island biomes
- the chart (fed from the registry)
- discovery
- NPC, enemy and boss spawns
- quest unlocks and the quest compass
- travel, and the Marines' spawn on Redbrick Port (found by name)

**Old worlds.** Worlds created before layout 19 keep the places they were generated with. Their islands are already
built there, and moving them would leave the old structures behind and put empty ocean where the new ones belong.
New worlds get the corrected places from the start, with no leftover or duplicate structures.

Tested by `GrandLineIslandSwapGameTests`, which checks:
- the recorded old positions and the swap;
- nothing else moved;
- size, level and theme are kept;
- island ids are unique;
- no build overlaps another in either layout.
It was also checked in game, in an old world and a fresh one.
