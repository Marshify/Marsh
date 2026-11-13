# Supplying island builds (structures)

Grand Line places structures you build in Minecraft into the Grand Line sea as chunks generate. Nothing in the
current First Sea is replaced until you ask for it.

## What to send per build

1. **The structure file(s):** `.nbt` files saved with a Structure Block (SAVE mode) in Minecraft Java 1.20.1.
   - One Structure Block can save at most 48 × 48 × 48. Split bigger islands into several pieces (send all of them
     and note how they line up, or give each piece its own placement — see below).
   - Include air where you need air (interiors, tunnels, rooms below sea level): the saved air is placed exactly,
     water included. Use `structure_void` only where the world should be left as it is.
2. **Markers inside the build:** a Structure Block in **DATA** mode, with one of these names in its data field.
   Markers are removed when the structure is placed (they become air; `dock` markers leave the sea untouched).

| Marker | What it does |
|---|---|
| `origin` | The point that the placement's `position` refers to (put it at the main dock or the island centre at sea level). Without it, the build's lowest north-west corner is used. |
| `dock` | A boat spawns floating on the water here (once per world). Put it in the water beside a pier. |
| `npc:quest_giver`, `npc:fruit_dealer`, `npc:fisherman`, `npc:navigator` | That NPC spawns standing here (once per world). Put the marker on the floor block's air space. |
| `arrival:<name>` | A travel landing called `<name>` (needs a solid floor and two blocks of headroom; the mod searches a few blocks up/down if not). |
| `quest` | Quest area (recorded for quests; no spawn). |

3. **Travel plates (optional):** a `stone_pressure_plate` flush with the floor, on top of an impulse command block
   (Needs Redstone) with the command `grandline travel @a[dx=0,dy=1,dz=0] <arrival name>`. Only the player standing
   on the plate travels. If a server has command blocks off, Grand Line runs the same plate itself (never both).
   Keep landings at least 3 blocks from any plate.
4. **Placement info** (one line per build is enough; I write the JSON):
   - which island it is for (its name in Grand Line, e.g. "Millbrook Village"),
   - where the `origin` marker should be in the world (x, y, z) — or "at the island's current anchor",
   - rotation (none, clockwise_90, 180, counterclockwise_90) if it should be turned.

## How a placement is written

`data/grandline/structure_placements/<name>.json`:

```json
{
  "template": "grandline:islands/millbrook",
  "position": [630, 62, 120],
  "rotation": "none",
  "mirror": "none",
  "island": "Millbrook Village"
}
```

The `.nbt` goes to `data/grandline/structures/islands/millbrook.nbt` (template `grandline:islands/millbrook`).

## Worked example

`tools/make_test_structures.py` builds the test data pack in `dev/test-datapacks/grandline_structure_test`
(summit plate → Upper Sky landing and back; sea-stack mooring → sealed stair → glass chamber 10 blocks below sea
level). Copy that folder into a test world's `datapacks/` folder before creating the world to see it.

## Guarantees (tested)

- Each chunk places its slice once, when first generated; existing chunks and player builds are never touched again.
- NPCs and boats from markers spawn once per world (saved), never again after reloads or restarts.
- Sealed rooms stay dry across chunk borders and restarts.
