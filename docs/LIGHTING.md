# Stylized world lighting (0.41.0)

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
