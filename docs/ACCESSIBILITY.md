# Island accessibility (0.67.0)

Every First Sea island can be reached from a ship and walked to its quest area without a single jump.

## What changed

- **Boat landings.** Each dock has a landing stage one block above the water (y 64), reached by stairs from the dock
  and lit by lantern posts at its corners. A sloop's deck sits at about y 63.6, so stepping off is a 0.4-block step.
- **Stepping off ships.** Pirate ships now pick the nearest dry spot within three blocks of the hull (dock, landing,
  beach), lowest climb first. Vanilla boats tried only one point and often left you standing on the hull beside a dock.
  On open water nothing changes: you stay on deck.
- **Paths without jumps.** Full-block ledges on the walk from dock to quest giver were replaced by stairs or half-step
  rows in each island's own materials.

| Island | Fixes |
|---|---|
| Millbrook Village | spruce landing; step row at the village edge |
| Sunscorch Dunes | jungle landing; steps up to the plaza |
| Gladiator's Ring | dark oak landing with stone brick posts; dock entry stairs |
| Emberfall Isle | landing; dock entry stairs; steps inland |
| Redbrick Port | landing on the south quay |
| Ironhold Bastion | stone landing at the east gate |
| Stonewall Prison | landing on the south pier |
| Saltmarket Docks | landing off the promenade |
| Crystal Fountain | landing at the pier; pier entry stairs |
| Frostpeak Isle | landing; walkway entry stairs; four step rows up the slope |
| Verdant Wilds | four mossy cobblestone step rows |
| Lotus Palace | two sandstone step rows (to the whirlpool islet) |
| Cloudtop Isles | three beach step rows; entry steps onto both rope bridges |
| Dawnreach Harbor | boarding gaps in the pier side rails |

## Checking an island

`/grandline admin access <island|all>` (operators) walks the route from boat water to the quest giver (or home spot)
and back, then reports the boat route, deck-to-landing height, path length and any jump still needed, with its
position and direction. All 14 First Sea islands report OK.

## Existing worlds

The island plans changed, so the fixes appear in chunks generated from 0.67.0 on. Already generated island chunks keep
their old blocks; nothing is overwritten. The ship disembark change works everywhere.
