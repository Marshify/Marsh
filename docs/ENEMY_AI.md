# Enemy combat AI: territory, aggression, pursuit and safe areas (0.60.0)

Every hostile Grand Line NPC (Bandit, Raider, Corsair, the island captains and race-trial waves) uses one server-side
territory AI (`GrandLineEnemy`). Vanilla targeting is gone: its target goals are removed, and `setTarget` refuses any
target the AI did not choose itself. The Saber Warden keeps its own arena rules: it fights whoever enters its temple
hall and resets when they leave.

## Behaviour
- **Idle.** It stays within `idleRadius` of its spawn spot, wandering a little and looking around, and walks back if pushed out. A spawn spot on a campfire, magma or similar is moved to the nearest safe ground.
- **Aggression.** It attacks a player who comes within `aggressionRadius`.
  - The player must be in sight (`lineOfSight`); never through walls or terrain.
  - The player must not be in a protected area, and must stand inside its pursuit area.
- **Provocation.** Hitting it from any distance provokes that enemy only, unless `groupAggression` is on (then neighbours within 8 blocks join).
- **Pursuit.** It gives up when:
  - it is `pursuitRadius` from its spawn spot (measured from the spot);
  - the target enters a protected area;
  - the enemy itself walks into one;
  - the target dies, disconnects, changes dimension, or turns creative or spectator.
- **Reset.** On giving up it drops its target and attack and walks home. On the way it ignores players, unless one inside its territory attacks it. It heals to full on arrival (`healOnReset`) and idles again. If it's stuck for 20 seconds it is put back.
- **Protected areas** (`SafeZones`, `protectionRadius`) cover:
  - every friendly NPC: quest givers, merchants, ability teachers, innkeepers;
  - the island's planned NPC spots, docks and arrival point;
  - the world spawn.

  Enemies never notice, chase or attack players inside them. A player inside one can't damage enemies either ("You can't attack from a safe area"), so they can't be used to fight without risk. New spawns are kept out of them.

## Settings (`config/grandline.json` → `enemyBehavior`, per kind)
| | bandit / raider / corsair | captain (island bosses) | trial (race trial waves) |
|---|---|---|---|
| aggressionRadius | 3 | 6 | 40 |
| idleRadius | 3 | 4 | 6 |
| pursuitRadius | 20 | 28 | 64 |
| protectionRadius | 8 | 8 | 0 |
| lineOfSight | true | true | false |
| groupAggression | false | false | false |
| healOnReset | true | true | false |

Missing entries are filled with these defaults; values are clamped.

## Unchanged
Spawn spots, respawn timers, levels, health and damage scaling, rewards, quests, loot, and friendly NPCs. Existing worlds
work as they are: enemies adopt the new rules when they load. The quest boss on islands without a boss arena now spawns
clear of the quest giver's protected area.

Tested by `GrandLineEnemyAiGameTests`: notice range; walls; single-enemy provocation; refused outside targeting; the pursuit limit with reset and heal; protected areas both ways; invalid targets. Also checked in game on an existing world.
