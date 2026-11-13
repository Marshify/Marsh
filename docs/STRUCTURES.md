# Island builds: what the importer supports

Grand Line places structures you build in Minecraft Java **1.20.1** into the Grand Line sea. This page describes the
importer exactly as the code works today (`world/structure/`), including what is not finished yet.

**Nothing in the current First Sea is replaced.** A build is placed on top of whatever the world generator makes at
that spot, so for now put builds in open sea. Replacing a generated island with your build is a separate step that
waits for your go-ahead.

## Working example: Example Cove

`dev/example-datapacks/grandline_example_cove` is a complete, tested data pack (made by
`tools/make_example_island.py`). It has two pieces:

| File | What it shows |
|---|---|
| `structures/examples/example_cove.nbt` | Grass islet, hut with a **Fisherman**, a pier with a **boat dock**, an `origin` marker at the pier tip, two **travel landings** and a **travel plate** (grass → pier). |
| `structures/examples/example_cove_lighthouse.nbt` | A lighthouse rock north of the islet: the **second piece** of a split island (no origin marker, placed by its corner). |
| `structure_placements/examples/example_cove.json` | Puts the pier tip at world (240, 64, -880). |
| `structure_placements/examples/example_cove_lighthouse.json` | Puts the lighthouse corner at (224, 56, -899), lined up with the islet. |

Verified on the dev server: both pieces line up, the Fisherman spawns in the hut, the boat floats beside the pier, the
plate sends you to the pier and `/grandline travel <player> example_cove` lands on the grass.

To see it yourself: copy the `grandline_example_cove` folder into `saves/<your Grand Line world>/datapacks/` **before**
going near (240, -880) (or add it on the Create World screen with **Data Packs**), then fly there.

## Exporting a build (Minecraft Java 1.20.1)

1. **Build at the real sea level.** Grand Line's water surface is **y 62**, and standing on a block at y 63 puts you
   just above the water. Build in water at that level (a normal ocean works). The saved area records the water, so the
   build drops into the sea cleanly. Any **air** you save below y 62 becomes a dry hole in the ocean. That is right for
   rooms and tunnels, but fill sea you don't want with `structure_void` (it means "leave the world as it is").
2. Get the blocks: `/give @s structure_block`.
3. **Add markers** (next section): place a Structure Block, click it, set the mode to **Data**, type the marker name
   in **Custom Data Tag Name**, then **Done**. Put markers in the air space where the thing should be, not inside
   solid blocks.
4. **Save:** place a Structure Block just outside a corner of the build, set the mode to **Save**, and fill in:
   - **Structure Name:** `grandline:islands/<name>`, e.g. `grandline:islands/millbrook`.
   - **Relative Position:** the offset from this block to the build's corner, e.g. `1 0 1`.
   - **Structure Size:** up to **48 × 48 × 48** (the 1.20.1 maximum).
   - **Include entities:** **Off**, unless you want armor stands or item frames saved (they get placed too). Never
     save mobs.
   
   Turn on **Show bounding box** to check the area, then press **SAVE**.
5. The file is written to
   `.minecraft/saves/<world>/generated/grandline/structures/islands/<name>.nbt`. Send me that file.

## Where the files go

| File | Location |
|---|---|
| Structure | `data/grandline/structures/islands/<name>.nbt` (template id `grandline:islands/<name>`) |
| Placement | `data/grandline/structure_placements/islands/<name>.json` |

These paths sit either inside the mod (shipped to everyone; I add them) or inside a world data pack
(`saves/<world>/datapacks/<pack>/data/...` plus a `pack.mcmeta` with `"pack_format": 15`) for testing on your own.

## Placement origin and rotation

```json
{
  "template": "grandline:islands/millbrook",
  "position": [630, 64, 120],
  "rotation": "none",
  "mirror": "none",
  "island": "Millbrook Village"
}
```

- **`position`** is where the build's **`origin` marker** lands in the world. Put that marker somewhere easy to
  reason about, e.g. the standing space at the main pier tip (y 64 when the deck is at y 63). Without an `origin`
  marker, `position` is the build's bottom north-west corner.
- **`rotation`:** `none`, `clockwise_90`, `180` or `counterclockwise_90`. The build turns around the `origin`
  marker, so the marker stays exactly at `position`. **`mirror`:** `none`, `left_right` or `front_back`.
- **`island`** (optional): the exact Grand Line island name, e.g. `"Millbrook Village"`. NPCs from this build then use
  that island's level and quests; without it they count as a level 1 island.

## Markers

A Structure Block in **Data** mode whose data name is one of the following. Markers never appear in the world: they
turn into air (a `dock` marker leaves the sea water untouched).

| Marker | Effect | Status |
|---|---|---|
| `origin` | The point `position` refers to; rotation pivots here. One per build. | Works |
| `dock` | A boat spawns floating here, once per world. Put it in the water beside a pier. | Works |
| `npc:quest_giver`, `npc:fruit_dealer`, `npc:fisherman`, `npc:navigator` | That NPC spawns standing here, once per world. | Works |
| `arrival:<name>` | A travel landing called `<name>`. Needs a floor and two blocks of headroom (the mod searches a few blocks up and down if not). Names must be unique. | Works |
| `quest` | Nothing yet: the marker is read but no code uses it. | Not finished |
| Boss arena | **No marker exists.** Bosses still come from the hard-coded arena on each island. | Missing |

**Travel plates:** put a `stone_pressure_plate` on top of an impulse command block (Needs Redstone) whose command is
`grandline travel @a[dx=0,dy=1,dz=0] <arrival name>`. Only the player standing on the plate travels. If a server has
command blocks off, Grand Line runs the plate itself (never both). Keep landings at least 3 blocks from any plate.

## Splitting a big island into several files

Each Structure Block saves at most 48 × 48 × 48, so a big island becomes several pieces, each with its own `.nbt` and
its own placement JSON:

1. Build the whole island in one world. Split it into boxes of at most 48 per side, with no overlaps.
2. Put the single `origin` marker in the **main** piece only. The other pieces have **no** `origin` marker; each one
   is placed by its own bottom north-west corner.
3. For every piece, write down the build-world coordinates of its bottom north-west corner. That is the Save block's
   own position plus its Relative Position. Also write down where the `origin` marker sits.
4. Send me the files and those numbers. Each extra piece's `position` is
   `origin position + (piece corner − origin marker)`, all in build-world coordinates. The Example Cove lighthouse is
   placed exactly this way.

Keep `rotation` at `none` for split islands: each piece would turn around its own corner and the pieces would drift
apart (see below).

## Guarantees (tested)

- Each chunk places its slice once, when first generated. Chunks that already exist, and anything players built in
  them, are never touched again.
- NPCs and boats from markers spawn once per world (saved), never again after reloads or restarts.
- Sealed rooms stay dry across chunk borders and restarts.

## Not finished yet

1. **Boss arenas:** no marker; boss arenas are still built by code.
2. **`quest` marker:** read but unused.
3. **Split islands are not linked:** pieces are placed independently, so a multi-piece island can't be rotated or
   moved as one, and the positions are worked out by hand. Overlapping pieces have no defined order.
4. **New chunks only:** a build appears only where the world hasn't been generated yet. There is no command to paste
   it into an area that's already explored (use a fresh world or unexplored sea to test).
5. **No "replace island" switch:** a build at an existing island's anchor is layered over the generated island.
   Replacing islands is paused until you ask.
6. **Builds are not islands yet:** a standalone build doesn't appear on the M chart, isn't discoverable, and has no
   name banner or level of its own. Only its NPCs link to an island, through the `island` field.
