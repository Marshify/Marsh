# Sunscorch Dunes rebuild (0.28.0)

Handcrafted desert island, Lv 75 (level unchanged; the First Sea rebalance comes next). Generated from code in `world/plan/SunscorchDunes.java`.

## Layout (island-local: +x east, +z south; sea level 62, village ground 66)

| Area | Where | What |
|---|---|---|
| Pier and arrival plaza | south, x 8, z 48-84 | 31-block jungle-wood pier on log posts, rope rails, lanterns, T-head with cargo; sandstone plaza with gateway posts; dockside shelter |
| Market square | centre (0, 16) | patterned plaza, roofed well, six striped stalls with goods, benches, pottery, a cart |
| Village | around the market | inn (2 floors + roof terrace), trading house with walled courtyard, six homes (elder, potter, family, weaver, scholar, fisher) |
| Great Pyramid | north-west (-18, -48) | 33 wide, 22 layers + gold capstone (top at y 92); pillared gateway, lantern-lit corridor, 11 x 11 altar chamber |
| Lesser Pyramid | north-east (28, -50) | 23 wide, 15 layers, weathered (missing stones, sand drifts); burial room with a sarcophagus |
| Raider ruins | (8, -27), between and south of the pyramids | broken enclosure walls, pillars, fallen column; combat area + boss arena |
| Bandit camp | east (57, 5) | three tents, campfires, crates, cover walls, weapon rack; combat area |
| Oasis | west (-52, 10) | 2-deep pool, smooth grass ring, reeds, lily pads, ruined colonnade with seats, palms |
| Quest giver | east gate (32, 18) | between gate pillars under an awning, on the road to the bandit camp |

Every building is flat-roofed sandstone on a plinth: pilaster corners, base course, glass windows with jungle shutters and lintel hoods, projecting roof beams, a crenellated parapet, a striped awning porch, and a furnished interior. Doors stand open (adventure mode can't open doors). The inn and two homes have an inside stair and a front balcony; three single-storey homes have an outside stair to a roof terrace.

Roads: smooth sandstone main roads (dock-market, market-quest-camp, market-ruins), sandstone lanes (to both pyramids, the oasis, side lanes and each door), surfaces worn with cut and plain sandstone.

Detail: 22 curved palms (leaning trunks, drooping fronds, coconuts), lamp posts along the roads, boulder outcrops, dead bushes, sparse cacti, soft red-sand drifts on the dunes, exposed sandstone on steep dune faces.

## Size

Land: 153 x 133 blocks (x -77..76, z -71..62); with the pier, z reaches 84.

## Gameplay spots

Quest giver (32, 67, 18); Innkeeper (home) east of the inn; Fisherman on the pier T-head; boat at the pier; arrival on the plaza; enemy camps: bandit camp and raider ruins (radius 6, leash +6); quest bosses in the ruins; buried treasure on the west beach. The quest giver, innkeeper and arrival plaza are all at least 18 blocks beyond any camp's leash. Quests, enemy kinds and levels come from the island's level (75), unchanged.

## Existing worlds

The rebuild is world layout 5. Worlds created earlier keep generating the original Sunscorch (`FirstSeaPlans.SunscorchDunesLegacy`), so no saved terrain is touched; new worlds get the rebuild.

## Also fixed

Island NPCs whose spot has no fixed height were placed using the world height map; if that chunk wasn't generated yet they were put at the bottom of the world and lost (this is why a quest giver could be missing). They now stand on the plan's ground in that case. Older game tests give their mock players a race with no combat bonus, so random race rolls can't make them fail.

## Tests

- `GrandLineSunscorchGameTests`: older worlds keep the original island; every gameplay spot is present; quest giver, innkeeper and arrival are on land, at ground height and far from both camps; camps are on level ground; land size within 140-165 x 125-145.
- Full suite: 101 / 101 pass.
- In game (fresh world, dedicated server + client): aerial view, pier toward the village, market, both pyramids and the ruins, both pyramid interiors, inn ground and upper floor, trading house, oasis, bandit camp with enemies, quest giver at its post; NPC positions checked by command (quest giver y 67, innkeeper y 67, fisherman on the pier).
