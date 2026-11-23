# Ironhold Bastion (layout 20)

A naval fortress on a raised terrace to the north, with a timber-framed harbour town below it to the south.
The island stays in the same place. Worlds created before layout 20 keep the original island
(`IronholdBastionLegacy`) and its treasure chests. New worlds get this rebuild.

Plan: `src/main/java/com/grandline/world/plan/IronholdBastion.java`. Coordinates are relative to the island centre:
x runs east, z runs south, and sea level is y 62.

## Layout
| Area | Where | Ground |
|---|---|---|
| Fortress terrace (courtyard) | z −54 … 7 | y 70 |
| Town | z 8 … 48 | y 65 |
| Dock (forked) | x −5 … 5, z 49 … 62 | y 63 |
| East and west landforms (enemy camps) | x ±48, z −10 | grass hills |

## Fortress
- **Walls**: an eight-sided enclosure, 3 blocks thick and about 9 tall. Red brick on a polished granite base, with white
  concrete and quartz courses under white merlons, and a walkway on top. Small naval banners hang along the walls.
- **Great towers** (three in a triangle): west and east are 21 wide at the base, 16 at the top and about 34 tall. The central
  tower is 23 → 17 and about 39 tall. Each has clean white quartz tiers with ledges, dark window slits, a blue glass
  observation level (two on the central tower), a burgundy rounded dome and a blue flag.
- **Small towers** (six): about 9 wide and 16 tall, with corbelled crenellated platforms. Four carry blackstone and anvil
  cannons aimed out to sea.
- **Main south gate**: a 7×9 opening between round gateposts that are 3 across and 12 tall with crowns. A blue plaque
  sits on the lintel, golden doors stand open inward, and large blue anchor banners hang on gold-topped rods.
- **Side gates**: 5×7 openings in the east and west walls, with steps down to the landforms. A small postern gate sits
  in the north wall.
- **Courtyard**: paved stone connects all the gates. The boss arena is here.
- A wide quartz staircase leads from the town up to the main gate, between quartz cheek walls.

## Town
- Eight timber-framed houses with dark oak frames and different plaster (white, sandstone, birch, calcite). Roofs are
  orange, blue, brown or green, with chimneys, awnings and crest banners. The houses have exteriors only.
- **Plaza**: a 19×19 octagon of grey and blue rings with a four-lobed white outline and a blue and white diamond in the
  centre. The quest giver and the Home NPC stand here.
- A waterfront promenade with a white retaining wall, the light stone dock with its forked end, and two dark wooden watch
  platforms (6×6, deck at about 11, roof at 15; the east one has a cannon).

## Landscaping
Blocky trees, hedges along both terrace edges, lampposts, stone benches and coastal rocks (in shallow water only).
Lighting is hidden inside the towers and house ceilings.
There are no treasure chests, pumpkins, props or new NPCs, as the design asks.

## Notes
- Treasure: Ironhold has no chests in new worlds (`TreasureSpots.NONE_BY_DESIGN`). Old worlds keep their four.
- Minecraft has no dark blue concrete, so the darker blue ring of the octagon uses blue terracotta.
- Game test: `GrandLineIronholdGameTests` checks that old worlds keep the original island and checks the new terrain and
  anchors.
