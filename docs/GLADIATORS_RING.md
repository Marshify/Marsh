# Gladiator's Ring rebuild (0.29.0)

A grassy island with rocky shores, dominated by an oval colosseum; Lv 225 (unchanged until the First Sea rebalance). Generated from code in `world/plan/GladiatorsRing.java`.

## Colosseum (centred at island-local (0, -8))

- Outside: 92 x 80 blocks, 30 tall (ground 69, parapet top 99).
- Facade: three storeys of arches between polished andesite pilasters with chiseled capitals, smooth-stone cornices between storeys, a solid attic with chiseled panels, a crenellated parapet, weathered stone (cracked and mossy bricks).
- South gate: a 17-wide stair from the plaza, a four-column quartz portico with an entablature and a stepped pediment, red banners.
- Arena floor: 45 x 35, sand with a cut-sandstone ring and a smooth-sandstone border; open sky, no obstructions. Quest bosses fight here.
- Podium wall to 4 blocks above the floor, railing, lanterns and red banners facing the arena.
- Seating: lower tier (8 rows), a mid walkway, upper tier (6 rows); every row is a stair (walkable), with stone aisles every 7 bays.
- Galleries: the ground ambulatory, the upper gallery and the top promenade behind the seating; four continuous stair flights (18 steps, at the diagonals) connect them, each leaving a 2-wide passage beside it.
- Four entry tunnels at ground level (the south one 7 wide, lantern-lit).
- Underground (floor y 59): the preparation room (benches, smithing table, grindstone, anvil, weapon rack, barrels) with a stair up to the arena floor; a corridor of ten holding cells (iron-bar fronts, benches, hay beds); a pillared training hall (dummies, targets, racks). A stair leads down from the ambulatory beside the main tunnel.

## Around it

- East: training grounds (25 x 25 gravel courtyard, low wall with gates, corner columns with lanterns, dummies, two raised platforms, an obstacle wall, racks, targets). Combat area.
- West: ancient warrior grounds (broken mossy paving, three raised round platforms, broken columns, a fallen column, a ruined arch). Combat area.
- South: the entrance plaza (statues, benches, the quest giver's post), a broad stone-brick road to the settlement and pier.
- Settlement: barracks (beds, racks, dummy), supply shop (counter, shelves, racks, smithing table), tavern (bar, tables, upstairs room), two houses (one with a walled courtyard). Stone plinths, white plaster in dark-oak frames, red brick-tile gable roofs; doors stand open.
- Pier: 30 blocks, stone footings, rails, lanterns, a T-head with cargo, a guard shelter on the landing.
- Small ruins: a broken colonnade on the north path, a ruined wall, a monument. Cypresses and a few oaks, lamp posts, stone and cobble outcrops in the grass.

## Size

Land about 165 x 150 blocks (x -80..84, z -77..73), the pier to z 92.

## Gameplay spots

Quest giver on the plaza (11, 67, 38); Innkeeper by the tavern (8, 67, 55); Fisherman on the pier; boat at the pier; arrival on the landing; enemy camps: training grounds and warrior grounds (radius 8); quest bosses on the arena floor; treasure on the west shore. Camps are outside the colosseum and far from the quest giver, innkeeper and arrival.

## Existing worlds

World layout 6. Older worlds keep generating the original ring (`FirstSeaPlans.GladiatorsRingLegacy`); no saved terrain is touched.

## Tests

- `GrandLineGladiatorGameTests`: older worlds keep the original island; every gameplay spot present; safe spots on the ground and away from camps; camps on level ground and outside the colosseum; boss spot on the arena floor; land size 150-175 x 135-160.
- In the generated world (by command): the 2-block passage beside all four stair flights is open (240 cells); every one of the 72 steps exists with headroom and the flights are continuous.
- In game: aerial, pier view of the facade and gate, arena and seating, prep room, cell corridor, main tunnel, training and warrior grounds, settlement; quest giver, innkeeper and fisherman positions; enemies present in both combat areas (none on the arena floor).
- Full suite: 104 / 104 pass.
