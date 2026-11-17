# Frostpeak Isle (Snow Island / Frozen Village), rebuilt (0.44.0)

Built to the owner's specification in `world/plan/FrostpeakIsle.java`, vanilla blocks only. World position unchanged
(630, -420). Island-local coordinates: +x east, +z south (the docks' coast). Sea level y 62.

## Dimensions used (blocks)

| Structure | Spec | Built |
|---|---|---|
| Island footprint | 128 x 112 | ~126 x 112 (x -64..62, z -58..52) plus peninsulas, sea ice and coastal spires |
| Village plateau | 52 x 42 | ellipse 56 x 46 (x -3..53, z -32..14) plus a southern lobe for the ladder top |
| Arena outer diameter / ring | 43 / 3 | 43 (radius 21.5, varied by up to +-1.1) / 3 |
| Frozen lake | 19 x 17 | 19 x 17, one block below the basin |
| Small houses | 9 x 11, walls 5, roof 4 | 9 x 11, walls 5, roof rise 4 (N1, N2, S3) |
| Large houses | 11 x 14, walls 6, roof 5 | 11 x 14, walls 6, roof rise 5, a 2 x 2 chimney 2 above the ridge, a porch (N3, S1, S2) |
| Watchtowers | 6 x 6 x 17, platform +14 | 6 x 6, platform +14, hipped roof to +19 |
| Large / medium ice crystals | 20-30 / 9-17 tall | 22, 24, 26, 28 / 9-16 |
| Rock spires | 12-25 tall | 12-24 |
| Docks | 17 x 5 and 15 x 5 at +2 | 17 x 5 and 15 x 5 at y 64, 7 wide over their last 3 blocks |
| Market stalls | 6 x 4 x 4 | 6 x 4 x 4 (x -14..-9 and -6..-1, z 41..44) |
| Cliff staircase | 23 span, ~13 rise | 18 span (walkway x -4..-1, stairs x 0..5, platform x 6..13), stairs rise 6, ladder 5, total 12 |
| Cavern | 34 x 28, 9-13 high, floor ~5 below sea | 34 x 28 (irregular), floor y 57, ceiling 9-13 above (never closer than 3 blocks to the surface) |

## Landmarks (local x, z; surface y)

| Landmark | Position |
|---|---|
| Arena centre | -24, -17; 82 |
| Village (main road) | 23, -3; 81 |
| Primary dock | x -14, z 48..64; 64 |
| Secondary dock | x -1, z 48..62; 64 |
| Market stalls | around -7, 43; 66 |
| Staircase | walkway -4, 22 (70) to platform 6..13, 20..23 (76); ladder 12, 20 up to 81 |
| Giant crystal cluster | -43, -35 (28 tall), plus -28,-46; -54,-20; -6,-50 |
| Cavern centre | 5, 8; floor 57 |
| Tunnel mouth | -19, 38 (lower coast, west of the stalls) |
| Terraces | lower coast 66, first 69, middle 73, upper 77, ridge up to ~89 |

**NPCs:** quest giver (22, 82, -1) and innkeeper (16, 82, -7) on the village road; fisherman at the end of the primary
dock; arrivals on the dock landing (-7, 65, 47); basic enemies in the arena, stronger ones in the northern wilderness
(18, -44); quest bosses in the ice cavern (0, 58, 8), beside the campsite.

**Routes (walk-tested in adventure mode):** dock landing -> lower coast (stairs); lower coast -> first terrace (stone
steps beside the stalls); cove -> scaffold stairs -> upper platform -> ladder -> village plateau; village <-> arena (brown
road); lower coast -> tunnel -> cavern.

**Deviations:** treasure chests are left out, as the spec asks (old worlds keep theirs). The staircase span is 18 rather
than 23. Tower roofs reach +19 rather than +17. The X braces are fence blocks (vanilla has no thin diagonals). Snow on
roofs is snow blocks and snow layers over stepped roofs. Some coastal ice is on open water next to the shore, not on land.

**Existing worlds:** a world made before 0.44.0 (layout 10 or lower) keeps the original Frostpeak, its NPCs and chests;
nothing is regenerated. New worlds get the rebuild. To see it in an existing world, the island's region files would have
to be deleted so they generate again. That is destructive, so it is not done automatically.
