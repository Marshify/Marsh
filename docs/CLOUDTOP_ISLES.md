# Cloudtop Isles: the sky islands (0.45.0, layout 12)

The owner's own Sky Island design: an archipelago of floating islands among big rounded clouds, joined by narrow pale
bridges with dark edges, with translucent sky arches hanging in the air. Two progression regions, as before (lower
Lv 150-190, upper Lv 450-625; see FIRST_SEA_PROGRESSION.md). Generated from code in `world/plan/CloudtopIsles.java`;
same island id and position (-1500, -540).

## Layout (island-local; ground level)

| Piece | Centre, size | y | Contents |
|---|---|---|---|
| Cloudfoot Cove | (0, 30), ~60 x 40 | 66 | arrival green, the pier (fisherman, dock), birches, rocks, flowers, lamp posts, the pool the waterfall falls into |
| Mistfall | (28, -4), 26 x 20 | 98 | the waterfall island: rocks, two oaks, a stone-rimmed spring feeding the spout, the waterfall straight down to the cove's pool; lower camp (basic) |
| Bloomhold | (-44, -40), 60 x 48 | 100 | the grassy village: plaza with a fountain, benches and lamps; six colourful cottages (coloured walls, coloured stair roofs, birch frames, furnished, doors); three round towers with blue, orange and teal domes, gold finials, a ladder up to a lookout floor; paths to every door; trees, rose bushes, flowers; lower quest giver and home |
| Azure Rise | (36, -64), 46 x 46 | 114 | the temple: a stepped quartz platform, columns all round, a cella with an altar, a blue frieze, a pale gable roof with **blue pediments** (a pale ring and gold in each), gold at the apex; behind it the **curved arched colonnade** (a half ring of piers and round arches, blue band, cornice, gold-capped ends); in front a pale plaza, the boss ring, the stronger lower camp and the **sky arch up** |
| Skyreach Citadel | (-10, -142), 92 x 68 | 200 | the castle (the whole upper region): weathered stone (cracked, mossy), a **big blue dome** with light ribs and a gold lantern on top, two wings with blue-green hipped roofs (halls with a balcony and ladder), four corner spires and two entrance spires with tall pointed caps and gold tips, the entrance hall with an arched doorway and spiral banners; the courtyard (trees, lamps, the upper quest giver, two upper camps) and the **return arch** at its edge |

### The domed chamber

Round hall, inner radius 12, floor y 200, walls to 221, the dome above (to about 236). Tall light blue windows every
30 degrees; doorways south (entrance hall), west and east (the wings). A dark-plank **gallery** at y 210 round the wall,
railed, reached by **three ladders**. **Spiral banners** (blue, with a curly border, a ring and a whirl) between the
windows. The **ring chandelier**: a dark ring of radius 6 at y 216, hung from the dome on four **chains**, eight soul
lanterns (blue light) underneath, chain spokes to a gold hub with a ninth lantern, and a chain up to the crown. The
**floor mechanism**: an outer copper ring with teeth, an inner ring of dark stone, eight copper spokes, the **gold hub**
in the middle. The upper boss fights here.

## Getting around

* The lower islands are walked: cove -> cloud platform -> Bloomhold; Bloomhold -> cloud -> Mistfall; Bloomhold -> cloud
  -> Azure Rise. Bridges are three blocks wide (quartz and calcite over a dark deepslate edge band), climb at most one
  block per block (stairs), and have a dark post with a lantern every ten blocks.
* The **sky arch** on Azure Rise (50, 115, -50) carries a player up to the citadel courtyard (-10, 201, -120); the
  **return arch** (-10, 201, -114) brings them back (45, 115, -50). Walk through; no keys, works in adventure mode.

## Sky

Five small islands below (grassy with a tree, or bare rock) and four up by the citadel; twelve big rounded clouds
(clusters of puffs; every block of a puff joins its middle, so nothing floats alone); four translucent arches of
light blue and white glass. Island undersides are thick clouds with lumpy rims (rim lumps only where they touch the
cloud next to them).

## NPCs, enemies, chests

* Lower: arrival (0, 36), fisherman and dock on the pier, quest giver (-40, 101, -34) and home (-49, 101, -36) by the
  Bloomhold plaza, camps on Mistfall (basic) and Azure Rise (stronger), the boss on Azure Rise's ring.
* Upper: quest giver (-4, 201, -122) in the courtyard, camps west and east of it, the boss in the domed chamber.
* Treasure chests (all outdoors, spread out): legendary on the citadel lawn by the west camp, rare between the temple
  and the colonnade, commons on Mistfall and in Bloomhold.

## Existing worlds

Worlds made with layouts 7-11 keep the first rebuild (`CloudtopIslesV7`, docs/CLOUDTOP_ISLES_V7.md) with its portals and
chests; worlds older than that keep the original. `IslandPlans` now has an "interim" plan for islands rebuilt twice.

## Checked

`/grandline audit Cloudtop Isles` on a fresh world: 0 problems. In game: both arches carry a player each way; all four
chests placed; the views from the cove, the bridges, Bloomhold, Azure Rise, the citadel courtyard and the chamber.
