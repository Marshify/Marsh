# Ironhold Bastion NPCs (0.69.0)

Ironhold keeps a fixed roster (`IronholdRoster`), enforced by `RosterNpcs`, the same system as Millbrook
(`IslandRoster`). Rebuilt Ironhold (layout 20) only; worlds on the original Ironhold keep their old NPCs.

## Marine Trainees: 7, Lv 1-2, melee, no bosses

One group in the fortress courtyard (the elevated training field), island-local spots:
(-24,-5) (-14,-8) (-5,-12) (5,-12) (14,-8) (24,-5) (0,-18).

- New enemy type `grandline:marine_trainee` (same strength as Millbrook's Bandits), drawn from the supplied skin on
  the full player model.
- Each has its own spot and respawns there 8 seconds after it falls, on its own timer; never two on one spot.
- Short leash (pursuit 12 blocks): they give up at the courtyard gate and never reach the street, merchants or docks.
- Old landform camps, the courtyard boss arena and any other hostiles are gone.

## Friendly NPCs: 8

| Name (names.json) | Role | Where |
|---|---|---|
| Marine Leader | Quest Giver | on the terrace just outside the main gate, facing the staircase |
| Fruit Dealer | Fruit Dealer | the street at the foot of the staircase, west side |
| Sword Dealer | Swordsmith | beside the east house, facing the plaza |
| Marine Boat Dealer | Boat Dealer (Sloop, Brig) | waterfront promenade, west |
| Marine Boat Dealer | Boat Dealer (Sloop, Brig) | waterfront promenade, east |
| Advanced Marine Boat Dealer | Luxury Boat Dealer (Galleon) | the main dock's west prong |
| Pirate Recruiter | join the Pirates | the plaza's west side |
| Set Home Point | Innkeeper | the main dock, beside the Marines' arrival point |

Removed from Ironhold: the Fisherman, the Movement Instructor and the Tinkerer (so the Cyborg race cannot be unlocked
until the Tinkerer is placed elsewhere).

## Quest

Defeat 5 Marine Trainees, from Lv 1, repeatable, with the same XP and Beli as Millbrook's starting quest.

## Existing worlds

On a player's first visit after updating: missing NPCs placed, the Marine Leader moved to the gate, removed roles and
old enemies deleted, the seven Trainees spawned. Nothing is rebuilt; Redbrick Port and other islands are unchanged.
