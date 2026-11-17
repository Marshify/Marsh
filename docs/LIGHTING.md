# Stylized world lighting (0.41.0, 0.41.1)

**Problem:** huge, nearly black regions on the ocean and terrain under floating islands (Cloudtop Isles, the upper
Skylands), and dark grey island undersides.

**Cause (checked):** vanilla sky light. Sky light only travels straight down from open sky and spreads sideways losing one
level per block, so a column under a floating island has no direct sky light; under anything wider than ~30 blocks it
falls to 0. The dark region is exactly the island's footprint projected down, which is that signature. It is not ambient
occlusion (that only darkens corners and creases), not face shading, not block light, and not a shader (the mod has
none). The server's light data is correct by vanilla's rules.

**Fix** (`client/lighting/StylizedLighting`, client-side, rendering only):
- When the game computes the light a block face, fluid or block entity is drawn with (`LevelRenderer.getLightColor`),
  and the sky light an entity is drawn with (`EntityRenderer.getSkyLightLevel`), a spot gets diffuse sky light 13
  instead of the propagated value if it is open air with 14+ blocks of air **above** it (the space under a floating
  island) or **below** it (the underside of the island itself).
- Rooms, halls, towers' floors and caves have a ceiling and a floor closer than 14 blocks, so they keep vanilla light: no
  glowing interiors or caves. Nothing below y 58 is changed (sea floor, underwater).
- It is still *sky* light: day, sunset and night darken it as usual. Block light (torches, lava, glowstone) is untouched.
  Particles, ability VFX, the sky, water rendering and entities otherwise render as before.
- Server light, world generation and gameplay that reads light (mob spawning, crops) are unchanged. Existing worlds just
  look brighter under islands.
- Cost: a short column check (at most 14 block reads, stopping at the first block) only for spots whose sky light is
  below 13, while chunks are meshed. Nothing runs per frame.
- Toggle: Settings, Visuals, **Bright skies** (on by default); switching it re-lights the loaded chunks.

**Tested** (before/after screenshots): from the side of Cloudtop, under the Sky Village, under the upper plateaus, looking
down on the ocean below the islands, at noon, sunset and night; inside the Saltmarket tavern and a house (unchanged); the
lighthouse shaft (not glowing).

**Not tested / limits:** with shader packs (Iris) or Sodium, which are not in the dev setup. Iris on vanilla rendering
uses the same light values, so it should follow. Sodium replaces the chunk-lighting code, so the ground under islands
would stay dark with Sodium until a Sodium-specific hook is added. There is no Underwater City in the game yet.

## 0.41.1: the islands themselves

**Problem:** the cloud sides of the floating islands rendered grey-black, and the Sky Village grass sat in a dark patch.

**Causes (checked by switching each off in turn):**
- The dark cloud sides came from **ambient occlusion**. Forcing full light changed nothing, but turning smooth lighting
  off fixed them. The stepped cloud surface is nearly all inner corners, and every vertex got darkened.
- The village patch was sky light again: the upper plateaus cast it. It was missed because the openness check treated
  short grass and flowers as solid.
- Fabric's **Indigo** renderer (the one that draws terrain with Fabric API) has its own AO switch and its own light
  lookup, so the vanilla hooks alone did not reach smooth-lit blocks.

**Fix:**
- Floating-island cloud (white wool and snow blocks above y 75) is drawn with flat lighting instead of AO. This is in
  both vanilla's `ModelBlockRenderer` and Indigo's `BlockRenderInfo`.
- The stylized sky light also applies in Indigo's `AoCalculator.getLightmapCoordinates`.
- Plants, torches and other non-solid blocks count as open space; leaves do not.

**Tested:** the cloud sides up close and from the side (noon and night), the Sky Village grass, the ocean below, and the
Saltmarket tavern and a house (unchanged). All of it is behind the same "Bright skies" setting.
