# Cloudtop Isles rebuild (0.31.0)

A sky archipelago in two progression regions (lower Lv 150-190, upper Lv 450-625; see FIRST_SEA_PROGRESSION.md). Generated from code in `world/plan/CloudtopIsles.java`; same island id and position.

## Layout and elevations (island-local; ground / walkway level)

| Section | Centre, size | y | Contents |
|---|---|---|---|
| Crescent Harbor | arc round a south-facing lagoon, ~100 x 60 | 66 | arrival landing and lagoon pier (fisherman, boat), two great grey rocks at the back + four smaller ones, freshwater lake with a wharf (the waterfall lands here), birches, flowers |
| Cloud Temple island | (-62, -26), 44 x 36 | 92 | pale calcite/smooth stone ground on a thick cloud; temple at the rear: 3-step podium, 9-wide stair, twelve columns, pillared hall with an altar, upper terrace (side stair), raised rooftop platform for a merchant; combat area in front (lower basic camp) and the lower boss spot |
| Forest island | (-40, -74), 28 x 24 | 106 | six oaks and birches, a flat-sided basalt rock, flowers, open middle (lower stronger camp) |
| Sky Village | (12, -60), 60 x 48 | 124 / 127 east terrace | six houses (plank or calcite, pitched roofs at varied heights, furnished, doors open), winding path, plaza with a well and benches (quest giver, innkeeper), steps between terraces, lanterns, trees, a rail along the edge by the plaza; the waterfall spills off the south edge into the lake |
| Sanctuary gate | (60, -100), 28 x 24 | 142 | weathered temple with a broken roof; inside, a quartz-ringed floor of cloud under an open shaft: the portal up |
| Upper plateau A | (-10, -40), 68 x 56 | 218 | four houses round a green, the upper quest giver's post, the return doorway (quartz arch), birches, the root bridge's west end |
| Upper plateau B | (88, -50), 100 x 80 | 226 | the great bell (gold and copper, on a 17-tall timber frame over a walled dais), the ancient temple (broad stair, eight tall columns, pillared hall, glowing dais, two side chambers, a spot for an instructor), the walled boss arena (34 x 24, no gaps), two upper camps |

Everything is below y 260 (the overworld builds to y 319; the vanilla cloud layer is at 192, so the sanctuary sits above it).

## Clouds

Every floating landmass rests on a solid cloud: land on top (grass or calcite, soil, stone), then a cloud body that deepens toward the middle (12 to 28 blocks) with lumpy rims bulging past the land's edge; white wool and snow outside, white concrete inside. Invisible light blocks under each cloud keep the undersides bright. Round cloud landings on the bridges and drifting cloud fragments between the layers.

## Route (no flight needed)

Crescent -> landing (y 78) -> Cloud Temple -> Forest -> landing (y 116) -> Sky Village -> Sanctuary gate: six stepped bridges, 3-wide pale stone with wall rails, lanterns and cloud puffs underneath, never more than one block up per block across. Gate -> Upper plateau A: the cloud portal; plateau A -> gate: the return doorway (both work in adventure mode, by walking in). Plateau A -> plateau B: the giant root bridge (dark oak wood, an arching, twisting root four wide on top with moss and rooted dirt, bark ridges as rails, hanging roots).

## Gameplay spots

- Lower region: quest giver in the village plaza; innkeeper beside it; fisherman on the lagoon pier; boat in the lagoon; arrival on the landing; camps on the temple island (basic) and the forest island (stronger); quest bosses in front of the Cloud Temple.
- Upper region: quest giver on plateau A (kept in place as a keeper NPC); camps on plateau B (basic west, stronger east); quest bosses in the plateau B arena.
- No boss existed for Cloudtop besides quest bosses, so none was added; the Cloud Temple's rooftop platform and the ancient temple's instructor spot are left free for a future merchant and instructor.

## Existing worlds

World layout 7. Older worlds keep the original Cloudtop (`FirstSeaPlans.CloudtopIslesLegacy`), which already has the upper region since 0.30.0 (west islet camp, summit quest giver and boss). Portals exist only on the rebuilt island.

## Tests

- In the generated world: all 117 walkway cells of the six bridges are solid with two blocks of headroom; the portal up lands on plateau A at its arrival point and the return doorway lands at the gate; quest givers (both regions), innkeeper and fisherman stand on the ground; enemies present on the forest island and plateau B.
- `GrandLineCloudtopGameTests`: older worlds keep the original island (with its upper region); both regions have a quest giver, two camps and a boss arena; the upper quest giver is far above the lower; every spot is within the build height; the portals connect the gate and the sanctuary.
- Full suite: 108 / 108 pass.

## Known issues

- Enemies share one island limit across both regions, so at any moment a region may have only a few; they respawn every 10 seconds.
- Some cloud edges still show a soft shadow at the rim.
