# Millbrook Village NPCs (0.68.0)

Millbrook keeps a fixed roster, managed by `MillbrookNpcs` from the data in `MillbrookRoster` (rebuilt Millbrook only;
worlds still on the original Millbrook layout keep their old NPCs).

## Bandits: 10, Lv 1-2, no bosses

| Field | Spots (island-local x, z) |
|---|---|
| Training meadow (east of the village) | (41,-1) (46,-6) (51,-1) (47,4) (42,5) |
| Bandit camp (south-east, by the tent) | (42,29) (46,23) (47,29) (52,33) (42,34) |

- Each Bandit has its own spot (tag `grandline.millbrook_spot.N`) and comes back **8 seconds** after it falls, at that
  spot, on its own timer. A spot never holds two Bandits.
- They idle at their spot, give up a chase about 20 blocks out, and never fight inside an NPC's protected area, so they
  stay out of the village and away from the dealers.
- Random enemy spawning, the shared respawn queue and boss arenas no longer apply to Millbrook.

## Friendly NPCs: 7

| Name (names.json) | Role | Where |
|---|---|---|
| Bandit Quest Giver | Quest Giver | where the two field roads fork, facing the village |
| Boat Dealer | Sloop, Brig | the west landing |
| Boat Dealer | Sloop, Brig | the north-east landing |
| Luxury Boat Dealer | Galleon | the main dock plaza |
| Marine Recruiter | join the Marines | the village green |
| Set Home Point | Innkeeper | the main dock plaza, by the arrival point |
| Sword Dealer | Swordsmith | outside the smithy |

The Fisherman and the Movement Instructor are no longer on Millbrook (the Dash tutorial is still on the Marines'
starting island).

## Quest

Defeat 5 Bandits (either field counts), from Lv 1, repeatable, with the usual stage-1 XP and Beli.

## Existing worlds

On a player's first visit after updating, missing NPCs are placed, misplaced ones moved, extras and removed roles
deleted, old Bandits without a spot removed and the ten spot Bandits spawned. Nothing is rebuilt.
