# Pirate harbor rebuild: implementation blueprint (Saltmarket Docks)

Status: **plan only, nothing built yet.** All dimensions are provisional, chosen for proportion; none are measured from a source.

## 1. The island in the codebase

| Item | Current value | After the rebuild |
|---|---|---|
| Island id | `Saltmarket Docks` (`SeaLayout` s1, theme HARBOR) | Unchanged |
| World centre | (−30, 540) | Unchanged |
| Neighbours | Dawnreach (0, 0) and Sunscorch (570, 540), each ~540 away | Room for an island ~230 across |
| Level range | Stage 3, Lv 30–60 (`FirstSeaProgression`) | Unchanged |
| Plan class | `FirstSeaPlans.SaltmarketDocks` (a small brick market, three piers, cranes, one camp) | Renamed `SaltmarketDocksLegacy` and kept for old worlds |
| Special roles | `RaceProgress.TRIAL_ISLAND`: the Trial Keeper is auto-placed near the main NPCs | Unchanged; it follows the new quest spot |
| Anchors | QUEST, FRUIT_DEALER, FISHERMAN, DOCK, 1 ENEMY_CAMP, BOSS_ARENA, TREASURE | Same types, plus ARRIVAL, HOME and a second ENEMY_CAMP |

The rebuild uses the existing generation path (`BasePlan`: Terrain + anchors + `build(Canvas)`), the same system as the other rebuilt islands.

Compatibility:
- The new plan is `world/plan/SaltmarketDocks.java`.
- `IslandPlans.REDESIGNED_IN` gains `"Saltmarket Docks", 10`, and `IslandRegistry.CURRENT_LAYOUT` becomes 10.
- Worlds made before the rebuild keep the old island; there is no regeneration over saved chunks and no duplicate island.
- Quest data, progression, the race trials and the fruit dealer all keep working, because they look the island up by id and find its NPCs by anchor.

## 2. Coordinates

- Island-local coordinates: +x is right (seen from the harbour), and +z is toward the harbour (front).
- Sea level is y 62.
- The harbour promenade is at y 64, the village platform at y 67, and the wilderness at y 66–72.

## 3. Overall plan (top view, front at the bottom)

```
 z −150 ┌──────────────── rugged coast, rock ledges ───────────────┐
        │   trees    CLEARING A        rocks  trees   CLEARING B     │
        │  (−45,−95) basic camp      ridge    (42,−98) strong camp   │
        │   rocks       trees       x −10..8    boss spot     trees  │
 z −35  ├───────────── landward wall (gate at x −4..4) ─────────────┤
        │ H15  H16 │      CENTRAL HALL (veranda south)   │ H17  TOWER │
        │  WINDMILL│      x −14..14, z −24..−4           │ H18 (52,−20)│
        │ (−40,4)  │ ────────── PLAZA x −20..20, z 0..26 ─│ H6 H7      │
        │ H1 H2 H3 │       trees in planters, pennants    │ H8 H9 H10  │
        │ H4  H5   │                                      │            │
 z 48   ├══ harbour wall: arches every 12, cannons, walkway y 79 ══════┤
 z 52   │                 promenade (y 64), cargo, benches              │
LIGHT-  │   pier P1 (x −20)          pier P2 (x 12)       pier P3 (x 44)│
HOUSE   │   z 67..104                z 67..110           z 67..84      │
(−100,64)── causeway                                                   │
```

## 4. Terrain

| Part | Shape (provisional) | Blocks |
|---|---|---|
| Whole island | An irregular polygon, roughly x −112..104 and z −152..70, with three rear headlands and a west spur for the lighthouse. Not circular, and no sandy beaches. | Grass ledges over dirt, then stone, andesite and tuff cliffs, stepped 1–3 blocks at a time |
| Village platform | x −74..71, z −33..48, level at y 67, edged by a 2–4 block stone retaining face | Smooth sandstone and packed-mud paving; grass only in planters |
| Promenade | x −80..68, z 52..66, y 64 | Smooth sandstone, sandstone slab edging, smooth stone trim |
| Lighthouse rock | Centre (−100, 64), radius ~13, separated by a 5-wide channel and reached by a stone causeway at z 60 | Stone, andesite and tuff, with a round paved top |
| Wilderness | z −35..−152, ground y 66–72 with gentle ridges | Grass, dirt layers at the edges; coarse dirt, dirt and packed mud in the clearings |

## 5. Walls (pink brick)

| Segment | Footprint | Height | Features |
|---|---|---|---|
| Harbour wall | x −74..71, z 48..51 (4 thick) | From the promenade y 64 to the walkway y 79, crenels to y 81 | 11 arches (5 wide, 9 high, round tops of stepped stairs) every 12 blocks; brick pillars between; pale stone arch borders; cannons facing the sea; blue banners; two ladders |
| West and east walls | x −74..−71 and x 68..71, z −33..48 | Walkway y 79 | Corner bastions with cannons; the west wall has a sea gate toward the lighthouse causeway |
| Landward wall | z −36..−33, x −74..71 | Walkway y 77 | A central gateway at x −4..4 (arched, open) into the wilderness; two side towers |

Wall material: vanilla bricks are noticeably redder than "muted pink". The options are in section 10.

## 6. Landmarks

| Landmark | Position | Size | Notes |
|---|---|---|---|
| Lighthouse | (−100, 64) | Base radius 7 (y 64–69, polished andesite); shaft radius 5 tapering to 4 (y 70–112, calcite and white concrete); balcony radius 6 at y 113 with dark oak railings; lantern room radius 3 (y 114–118); blue cone to ~y 128 | ~64 tall, well above the houses (~20). Narrow blue arched windows every 8 blocks; dark trim bands; a hanging dark banner below the balcony. |
| Windmill | (−40, 4) | Body 9×9 narrowing to 7×7, y 67–96, steep blue roof to ~y 106; axle at y 92 facing the harbour | Cream plaster with dark oak framing and diagonal braces; three rows of blue windows; stone base. Four open-lattice blue sails, each 15 long and 5 wide, with crossbars every 3. |
| Green tower | (52, −20) | 7×7, y 67–100, green pyramid roof to ~y 112 | Five floors with a stair; narrow blue windows on every floor; diagonal braces. It stands at the east edge, clearly separate from the windmill. |
| Central hall | x −14..14, z −24..−4 | Two storeys (y 67–75–83) under a broad orange roof to ~y 92; a flat terrace with railings on its east end; blue flagpole to ~y 100 | A veranda on the south face (z −3..1, dark oak floor, roof on posts) faces the plaza. Ground floor: the tavern hall. Upper floor: the kitchen. |

## 7. Houses

Each house has its own footprint, height and roof. None is a copy of another, and placement is fixed, not random.

| # | Footprint (x1,z1 → x2,z2) | Floors | Roof | Ridge axis | Extras |
|---|---|---|---|---|---|
| H1 | −64,10 → −54,20 | 2 | Orange | x | Chimney, a one-storey shed on the west |
| H2 | −52,14 → −44,24 | 3 | Green | z | — |
| H3 | −30,16 → −22,26 | 2 | Orange | x | An awning to the plaza |
| H4 | −66,26 → −56,38 | 2 | Green | z | Chimney |
| H5 | −50,30 → −40,40 | 1 + attic | Orange | x | Barrels, against the harbour wall |
| H6 | 24,4 → 34,14 | 2 | Green | x | An awning to the plaza |
| H7 | 38,2 → 46,12 | 3 | Orange | z | Chimney |
| H8 | 24,20 → 32,30 | 2 | Orange | z | — |
| H9 | 36,18 → 48,28 | 2 | Green | x | A crossing wing on the south |
| H10 | 50,30 → 62,40 | 2 | Orange | x | Against the harbour wall; chimney |
| H11 | −20,34 → −10,42 | 2 | Green | x | Front row by the wall |
| H12 | 6,34 → 16,42 | 2 | Orange | x | Front row by the wall |
| H13 | −66,−28 → −56,−16 | 2 | Orange | z | Back row; shed |
| H14 | −52,−28 → −44,−18 | 3 | Green | z | Back row |
| H15 | 22,−30 → 32,−20 | 2 | Orange | x | Back row; chimney |
| H16 | 36,−28 → 44,−18 | 2 | Green | z | Back row |
| H17 | 58,−6 → 66,4 | 2 | Orange | z | East edge, by the tower |
| H18 | −30,−20 → −22,−10 | 2 | Orange | x | Beside the windmill |

Shared details:
- Smooth sandstone plaster and stripped dark oak frames, with floor bands every 4 blocks and diagonal braces.
- Narrow blue windows framed in dark oak, and a stone brick foundation course.
- Pitched roofs with a 1-block overhang (acacia for orange, dark prismarine for green) and pale stone chimneys.
- Open doors (adventure mode) and simple furnishing: a bed, a table, shelves and lights.

## 8. Wilderness

| Feature | Placement (provisional) |
|---|---|
| Clearing A (basic camp) | An irregular polygon, centre (−45, −95), about 52×40 |
| Clearing B (stronger camp, boss spot at its far end) | An irregular polygon, centre (42, −98), about 48×46 |
| Green divide | x −14..10, with trees and the central rock ridge |
| Rock formations | R1 flat outcrop (−8, −70); R2 ridge (−2, −110), 14 long; R3 sea rock (−95, −60); R4 (88, −75); R5 (−70, −135); R6 (60, −140) |
| Tree clusters (handmade templates, fixed spots) | T1 (−85, −80); T2 (−20, −88); T3 (10, −130); T4 (80, −115); T5 (−55, −140); T6 (25, −55); about 22 trees in all, some leaning, with broad flat dark-oak-leaf canopies |

## 9. Interiors

- **Tavern hall (central hall, ground floor):**
  - Dark spruce and dark oak floor with a raised section up two steps.
  - Plaster ceiling panels between heavy beams on thick posts.
  - Round tables (fence post with a slab top) and stools.
  - Blue and red rugs (carpet), barrels, shelves and pottery.
  - An L-shaped counter under a red wool canopy.
  - Blue chains hanging from the beams.
  - Lattice display panels behind the counter.
- **Kitchen (upper floor):**
  - A black and white concrete checkerboard floor and a beamed ceiling.
  - Tall blue arched windows.
  - A stair up from the hall with railings round the opening.
  - A central island with a brick base and dark oak top, set with pots and plates.
  - A rounded brick oven.
  - Cupboards, shelves, hanging utensils and blue banners.
- **Other buildings:** the windmill and the green tower get stairs and plain floors. Houses get simple rooms.

## 10. Decisions before building

Several features in the spec need **custom blocks or models**. The mod's island builds so far use only vanilla blocks, so these are new work:

1. **Muted pink brick.** A new decorative block with its own texture, or vanilla bricks mixed with granite for a softer tone.
2. **Thin diagonal timber braces.** Custom thin models, or vanilla stairs and slabs set as diagonals (thicker).
3. **Windmill sails.** Custom thin lattice panels, or vanilla blue stained-glass panes and fences, which give an open lattice at full-block scale. Spinning sails need a block-entity renderer, which is extra work and better done after the static sails are approved.
4. **Cannons and pennant strings.** Small custom models, or vanilla stand-ins (a dark blackstone barrel on stairs; blue and white banners on chains or fences).
5. **Lattice display panels.** A custom model, or vanilla stand-ins (dark oak fences and trapdoors framing item frames with pottery).

Recommendation: build everything in vanilla first, verify it in the game, then add custom models only where the vanilla version clearly falls short. That keeps each step testable.

## 11. Anchors (NPCs and gameplay)

| Anchor | Spot |
|---|---|
| QUEST | Plaza, in front of the hall's veranda, at (0, 68, 6) |
| HOME (innkeeper) | At the hall's veranda, (10, 68, 3) |
| FRUIT_DEALER | West side of the plaza, (−16, 68, 18) |
| FISHERMAN | End of pier P2 |
| DOCK / ARRIVAL | Pier P2 / promenade at (0, 65, 58) |
| ENEMY_CAMP 1 (basic) | Clearing A |
| ENEMY_CAMP 2 (stronger) | Clearing B |
| BOSS_ARENA | The far end of clearing B |
| TREASURE | The rear west headland |

The Trial Keeper is placed automatically near the quest giver, as it is now.

## 12. Build order and checks

1. Terrain and coastline.
2. Walls.
3. Lighthouse, windmill and tower.
4. The hall and houses.
5. Harbour and piers.
6. Wilderness.
7. Interiors.
8. Decorations.
9. Compile, generate a fresh world, screenshot each area (overhead, harbour, lighthouse side, windmill side, wilderness, plaza, hall, kitchen) and walk-check every entrance and stair.
10. Add a game test, write docs, release a jar.
