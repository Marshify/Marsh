# Grand Line VFX library

One reusable library of visual effects for the whole mod (fruits, styles, swords, guns, bosses, races, techniques,
quests, progression). Effects are **data**: a JSON file per effect, made of timed layers, played by id.

## Playing an effect
- **Server (gameplay code):** `Vfx.play(entity, "grandline:combat/impact_heavy", pos, dir)`.
  - The full form also takes a scale, an entity to follow (anchor id) and a primary colour (`0xRRGGBB`, or `-1` for the effect's own palette).
  - Cosmetic only: it sends a beat to nearby players.
- **Client:**
  `VfxInstance.play(id, VfxInstance.Params.at(pos).dir(d).scale(1.5f).colors(0xFF5A3C, null).anchor(entityId))`.
- **Recolouring:** colours written as `$primary` / `$secondary` in a layer take the colours passed at play time, so one effect serves every palette.

## Effect JSON
- **Location:** `src/main/resources/assets/grandline/vfx/effects/<path>.json`
- **Id:** `grandline:<path>`, e.g. `combat/slash_steel.json` is `grandline:combat/slash_steel`.
- **Reloading:** effects reload with F3+T or `/glvfx reload`.

```json
{
  "category": "combat",            "description": "shown in the browser",
  "duration": 20,                   "render_distance": 96,
  "primary": "#FFFFFF",             "secondary": "#88CCFF",
  "layers": [ { "type": "sprite", "...": "..." } ]
}
```

### Common layer fields

| Field | Meaning |
|---|---|
| `delay`, `life` | Ticks |
| `texture` | Resource location |
| `frames` | `[columns, rows]` for a flipbook |
| `frame_time` | Ticks per frame; 0 plays the whole sheet over the layer's life |
| `blend` | `additive` (default; emissive) or `translucent` |
| `size` | Curve |
| `color` | Gradient |
| `alpha` | Curve |
| `offset` | `[right, up, forward]` relative to the effect's direction, in blocks × scale |
| `facing` | `camera`, `up` or `direction` |
| `rotation`, `spin` | Degrees, and degrees per tick |

- **Curves:** a number, a `[from, to]` pair, or keyframes `[[t, value], ...]` over normalised time 0..1.
- **Gradients:** `[[t, "#RRGGBB" | "$primary" | "$secondary", alpha], ...]`.

### Layer types

| Type | What it is | Extra fields |
|---|---|---|
| `sprite` | Billboard or plane, static or flipbook | |
| `particles` | Pooled emitter | `count` (burst), `rate`/tick, `lifetime` [min,max], `speed` [min,max], `spread`, `shape` (point/sphere/ring/disc), `radius`, `emit_axis` (direction/up), `inward`, `gravity`, `drag`, `spin`, `stretch` (velocity streaks), `collide`, `bounce` |
| `ring` | Expanding textured ring (shockwaves, circles) | `radius`, `width` curves, `segments`, `repeat`, `scroll` |
| `beam` | Energy beam or lightning along the direction, or to the target | `length` curve, `wobble`, `segments`, `taper`, `scroll` |
| `ribbon` | Smooth trail following the effect origin or anchor | `points` |
| `slash` | 3D crescent sword cut | `material` (metal/wind/fire/smoke), `radius`, `width`, `arc`, `sweep` (degrees curve), `tilt` |
| `mesh` | Custom OBJ mesh | `mesh` id, `texture` or `material` (wind/metal/fire/smoke shader surfaces), `spin`, `dissolve_from` |
| `builtin` | The procedural effects of the fruit reworks | `effect`: `explosion` (Bomb), `smoke_volume`, `vortex` (Smoke), `spike`, `crack` (Spike), `ground` (terrain debris and dust), `sparks`, `flash`, `crater`, `scorch`, `pressure_ring`, each with its own fields (`radius`, `power`, `count`, ...) |
| `distortion` | Heat-haze stand-in | |
| `sound` | Any sound id | `volume`, `pitch` |
| `shake` | Camera shake | `degrees` |

- **Distortion:** a faint shimmer instead of true screen-space distortion. That would need a framebuffer pass, which shader packs replace.
- **Sound:** at most 3 effect sounds per tick.

## Starter library (21 effects)

| Category | Effects |
|---|---|
| combat | `impact_light`, `impact_heavy`, `slash_steel`, `slash_wind`, `thrust_beam`, `dash_trail` |
| elemental | `explosion_small`, `explosion_large`, `fire_burst`, `smoke_burst`, `smoke_vortex`, `wind_vortex`, `spike_eruption`, `lightning_strike` |
| spatial | `portal_ring` |
| environmental | `dust_cloud`, `heat_haze` |
| transformation | `power_up` |
| aura | `aura_flare` |
| progression | `level_up`, `mastery_up` |

All are built from Grand Line's own textures, meshes and procedural renderers. None of the earlier Roblox packs are used.

## Importing your own artwork
1. Put files in `tools/vfx/incoming/<category>/`. Each asset is one of:
   - a transparent PNG;
   - a folder of numbered frames (`0001.png`, ...), packed into a sprite sheet automatically;
   - a sprite sheet with `"frames": [c, r]` in its sidecar;
   - an `.obj` mesh, optionally with a `.png` texture.
2. Add a sidecar `<name>.json`, or `_defaults.json` for the whole folder, with at least `"license"`, plus:
   - `"kind"`: `particle`, `flipbook`, `explosion`, `smoke`, `beam`, `lightning`, `slash`, `shockwave`, `magic_circle`, `aura`, `emissive`, `distortion` or `mesh`;
   - optionally `frame_time`, `blend`, `primary`, `secondary`, or a full `effect` definition.
3. Run `python3 tools/vfx/import_vfx.py`, or add `--check` to only validate. The script:
   - refuses assets with no licence or with an opaque background;
   - warns about sizes that aren't powers of two;
   - bleeds edge colours into transparent pixels so there are no dark halos;
   - copies textures to `textures/vfx/<category>/` and meshes to `vfx/meshes/<category>/`;
   - writes a starter multi-layer effect `grandline:imported/<category>/<name>`;
   - records the licence and source in `tools/vfx/manifest.json`.
4. Preview the new effect in `/glvfx`, then edit its JSON (or combine its texture into other effects) and press F3+T.

## Developer browser: `/glvfx`
Available in a development environment, or to operators (permission level 2).

- **Browsing:** effects are listed by category; clicking one plays it.
- **Buttons:**
  - **Play / Replay** and **Loop**.
  - **Scale** − and +.
  - **Colour:** cycles a primary-colour override.
  - **Quality:** Low, Medium, High or Ultra (the same setting as in game).
  - **Fallback:** forces the shader-pack fallback path, to compare with Grand Line's own shaders.
  - **Placement:** 5, 8.5 or 10 blocks ahead of you, or attached to you.
- **Diagnostics line:**
  - active effects, particles against the budget, and sprites;
  - the quality preset and the render path in use;
  - FPS and any load problems.
- **Commands:**
  - `/glvfx play <id> [scale] [#RRGGBB]`
  - `/glvfx list`
  - `/glvfx stats`
  - `/glvfx reload`

## Rendering, shaders and performance
- **Shader compatibility:**
  - Sprites, particles, rings, beams and ribbons use the vanilla position-colour-texture translucent and additive render types, so Iris/BSL draw them as normal translucent geometry.
  - Material surfaces (wind/metal/fire/smoke) use Grand Line's GLSL shaders; with a shader pack active they switch to the textured fallback automatically.
  - Both paths test depth and don't write it, so nothing draws through walls or turns opaque.
- **Quality presets:**

  | Preset | Particle multiplier | Mesh detail | Particle budget |
  |---|---|---|---|
  | Low | 0.3× | 0.5 | 400 |
  | Medium | 0.65× | 0.75 | 900 |
  | High | 1× | 1 | 1600 |
  | Ultra | 1.5× | 1.4 | 3000 |

  The maximum number of live effects is 48 + 32 × the preset index (0–3), so 48 on Low up to 144 on Ultra. Presets never affect gameplay.
- **Culling and cleanup:**
  - Effects past their render distance aren't started or drawn.
  - Effects behind the camera aren't drawn.
  - Particles are pooled and reused.
  - Instances clean themselves up and are cleared on world change.
