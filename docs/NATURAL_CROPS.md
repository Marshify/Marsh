# No natural pumpkins, melons or mushrooms (0.66.0)

Grand Line worlds no longer grow pumpkins, melons, red or brown mushrooms, or giant mushrooms on their own. It is done
at world generation, nothing scans or removes blocks afterwards.

1. **The sea biome.** `data/grandline/worldgen/biome/grand_line_sea.json` no longer lists `patch_pumpkin`,
   `brown_mushroom_normal` or `red_mushroom_normal`. Island chunks take their vegetation from this biome.
2. **A safeguard for the vanilla biomes given to islands** (plains, jungle, meadow…). `ConfiguredFeatureMixin`
   skips these features while a chunk of a Grand Line overworld is being generated (`world/NaturalCrops`):
   - `patch_pumpkin`, `patch_melon`;
   - `patch_brown_mushroom`, `patch_red_mushroom`;
   - `huge_brown_mushroom`, `huge_red_mushroom`;
   - `mushroom_island_vegetation`.

   It matches the features themselves, so every biome, sea and future island is covered. Skips are counted and logged
   when the server stops.

**Not affected:**
- **Players:** they can still place, farm and grow all of these. Bone-mealed mushrooms still become giant mushrooms (only placement during chunk generation is skipped).
- **Other worlds:** vanilla worlds, the Nether and the End.
- **Other vegetation:** trees, flowers, grass and sugar cane.
- **Island decorations:** carved pumpkin heads on Millbrook's scarecrows and the training dummies (Emberfall Isle, Gladiator's Ring), and the produce on Dawnreach's fruit shop and Sunscorch's market stall. They come from the island plans, not from features, and are deliberate.

**Existing worlds:** new chunks follow the rule straight away. Chunks generated before keep whatever grew there.

**Verified on a freshly generated world** (Millbrook, Verdant Wilds, Dawnreach, Lotus Palace and an open-sea area, about 450 chunks):
- No natural pumpkins, melons, mushrooms or giant mushroom blocks.
- Only the deliberate decorations remain (2 carved pumpkins on Millbrook, 1 pumpkin and 1 melon on Dawnreach's fruit shop).
- Grass, flowers and ferns are unchanged.

Game test: `GrandLineNaturalCropsGameTests`.
