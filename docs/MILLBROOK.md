# Millbrook Village (rebuild, layout 4)

New worlds use the rebuilt Millbrook. Worlds created before it (island layout 1–3) keep the original Millbrook exactly as it generated: nothing already in a world is regenerated or overwritten.

Island-local coordinates: +x east, +z south. The island centre is the registry anchor (630, 120 in a standard world).

| Feature | Local position |
| --- | --- |
| Island | about 135 × 123 blocks of land (measured in game), ground level 66 |
| Main dock | arrival plaza at (-3, 46); the pier runs south to z = 77, T-head z 72–77 |
| Arrival / new-player spawn | (-3, 46), facing the village |
| Village lawn | centre (-6, -2), radius 12, with the old oak and stone bench ring |
| Inn (Innkeeper outside at (-6, -19)) | (-6, -25), 2 storeys |
| Farmhouse | (-26, -14) |
| Weaver's cottage | (-28, 8) |
| Fisher's cottage | (-20, 23) |
| Smithy (Swordsmith outside at (14, 16)) | (12, 22), with an open forge |
| Bakery | (17, -9) |
| Elder's house | (12, -24) |
| West windmill | (-48, -6), on a knoll, sails facing west |
| East windmill and wheat field | (38, -28), sails facing east |
| Quest giver and notice board | (29, 12), facing the village |
| Combat field A (training meadow) | centre (46, 0), radius 9 |
| Combat field B (bandit camp); quest bosses appear here | centre (48, 28), radius 9 |
| West landing | the shore at z = 4 |
| North-east landing | the shore at x = 40 |
| Fisherman | on the dock's T-head (1, 72) |

**How it's generated**
- `world/plan/MillbrookVillage.java` describes the terrain analytically (land masses, levelled areas, roads) and builds every structure from code. Each chunk writes only its own blocks, so buildings, windmills and the dock join up across chunk borders and come out identical in every world.
- `IslandPlans` serves this plan to worlds of layout 4 and the old one (`FirstSeaPlans.MillbrookVillageLegacy`) to older worlds.

**Enemies**
- Enemies spawn only at the two field anchors, in groups as before.
- Island enemies now wander only within their camp: the camp radius plus 6 blocks.
- They still chase a player who engages them.
