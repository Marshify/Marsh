# Cloudtop Isles (0.46.0, layout 13)

The lower region (Lv 150-190) is the 0.45.0 sky islands with Skyreach Citadel moved down into it. The upper region
(Lv 450-625) is the new **Upper Cloudtop Isles**, built to the owner's specification and the approved blueprint
(docs/UPPER_CLOUDTOP_BLUEPRINT.md). Code: `world/plan/CloudtopIsles.java` (lower islands, citadel) and
`world/plan/UpperCloudtop.java` (the upper region).

## Lower region

As in 0.45.0 (see docs/CLOUDTOP_ISLES_V12.md): Cloudfoot Cove, Mistfall, Bloomhold and Azure Rise, joined by the narrow
pale bridges. In addition:

* **Skyreach Citadel** now floats in the lower region, centre (-150, 15), ground y 124, all of it as before (dome,
  wings, spires, entrance hall, the domed chamber). A bridge runs from Bloomhold's west edge over a cloud platform to it.
* The **lower boss** now fights in the citadel's domed chamber (-150, 124, 9).
* Azure Rise's sky arch now leads up to the Ancient Tree landmass.

## Upper Cloudtop Isles

| Piece | Where (local) | Notes |
|---|---|---|
| Ancient Tree landmass | centre (-95, -165), 145 x 125, ground 200 (settlement terrace 202, tree mound to 203) | irregular outline, rocky stretches round the rim, cloud banks hiding the rock, a deep rounded cloud underneath |
| Giant tree | trunk (-110, -170), 15 across | dark oak bark with log streaks and brown terracotta; eight angular branches; faceted canopy masses (green concrete, green and lime terracotta, a rim of oak and dark oak leaves); top y 289 |
| Roots | six from the trunk | the east root arches 7 blocks over the combat field (dark oak stairs on its slopes) and becomes the bridge; the others lie half-buried across the grass (paths cut through) |
| Settlement | round the plaza (-137, -146) | two white buildings, two yellow, a pink house, a small white house, the blue-domed and green-domed towers; narrow windows (black and grey panes, or dark openings with quartz sills), floors with ladders, doors |
| Combat field | x -97..-43, z -172..-128 | rocks, broken walls, a ruined arch, small trees; the upper basic camp |
| Classical pavilion | x -72..-48, z -198..-182 | 2-high plinth, columns all round, low grey roof, pink, magenta and red banners |
| Curved arcades | red (x -150..-121, z -206..-198), blue (x -115..-86, z -115..-107) | arched walls, barrel roofs; the red one's middle bay is the **return arch** |
| Root bridge | (-38, -165) to (27, -165) | ~60 long, 9 wide, rising 200 to 204, bark sides, no railings |
| Grand Sanctuary | centre (95, -165), 155 x 135, ground 204 | |
| Stepped temple | base x 78..142, z -217..-163 | five tiers of 7 with blue inlays (base band, stripe, squares, cyan corners), quartz ledges; top y 239 |
| Staircase | x 103..117, z -143 to -177 | 15 wide, one up per block, quartz walls with a blue line |
| Summit pavilion | x 98..122, z -198..-182 | columns, blue band, pediments with a blue tympanum, low grey roof, a hanging golden fixture |
| Ceremonial courtyard | x 85..135, z -152..-108 | grass in a quartz grid, pale approach and cross path, two rows of striped columns, two platforms; the **upper boss** |
| Golden bell | (88, -125) | 9 wide, 9 tall: gold rings, yellow concrete and terracotta bands, raw gold lip, light weighted plates, chains, a clapper; quartz frame 14 tall |
| Archways | (42, -166) and (55, -190) intact; (64, -214) leaning and ruined | |
| Tall buildings | east side | five white buildings 12-25 tall (blue, orange, flat and andesite roofs; one damaged) |
| Ruins, combat field | (44, -154), (146, -204); field round (62, -126) | the upper stronger camp |

## Gameplay

* Up: the sky arch on Azure Rise (50, 115, -50) lands you at (-135, 201, -192), facing the settlement. Down: the red
  arcade's middle bay (-135, 201, -202).
* Upper quest giver: the settlement plaza. Camps: the Ancient Tree field (basic, Lv ~470-510 seen) and the sanctuary
  field (stronger, Lv ~515-565 seen). Boss: the ceremonial courtyard.
* **Enemy spawning and keepers now follow large islands** (fixes a gap for every upper region): an island counts as
  near wherever a player is on its built area, enemies spawn at the camps within 128 blocks of the player, and the cap
  of four counts per camp area, so the lower region's enemies never block the upper camps.
* Treasure: legendary on the temple's summit platform, rare by the leaning arch, commons on the Ancient Tree field,
  Mistfall and Bloomhold.

## Shadows

Nothing hangs over another island. Since 0.45.1 floating islands cast no shadow anyway (see LIGHTING.md).

## Existing worlds

Layout-12 worlds keep the 0.45.0 sky islands, layouts 7-11 the first rebuild, older worlds the original
(`IslandPlans` keeps any number of in-between plans per island now).

## Checked

On a fresh world:
- The floating-block audit found 0 problems.
- Both arches carry a player each way, and all five chests are placed.
- The upper quest giver stands in place, and both upper camps fill with Raiders at their region's levels.
- Looked over in game: both landmasses from afar, the tree and its roots, the settlement, the pavilion, the root
  bridge, the temple and staircase, the courtyard and bell, the arches, and the moved citadel.

124 of 124 tests pass.
