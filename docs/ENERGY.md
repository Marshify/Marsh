# Energy (0.38.0)

**Max Energy = 100 + 5 × allocated Melee points** (plus any accessory Energy bonus, such as the Black Cape). Character level no longer adds Energy.

| Melee | Max Energy |
|---|---|
| 0 | 100 |
| 1 | 105 |
| 100 | 600 |
| 500 | 2,600 |
| 1,000 | 5,100 |
| 2,000 | 10,100 |
| 2,550 | 12,850 |

All in `combat/EnergyService`, server-authoritative:

- **Max Energy:** computed live from the allocated Melee stat.
  - Allocating Melee raises it at once and keeps your current Energy; the new room fills by regeneration.
  - A Melee reset clamps current Energy to the new max.
  - The HUD is updated immediately.
- **Regeneration:** unchanged at 8% of max per second, every server tick, never above max. There's no delay.
- **Costs:** each move keeps its existing cost, now as a fixed amount (its cost fraction × 100, what it cost at the starting pool). For example, Cinder Shots costs 7.
  - Race cost multipliers still apply.
  - Energy is checked before a move starts and spent only when it fires (charged moves are checked again on release).
  - Energy never goes below zero.
  - Moves that cost nothing still cost nothing.
  - Config `energyCostsScaleWithMax = true` brings back the old rule (cost = fraction of max).
- **Persistence:** Energy itself isn't saved. You join and respawn full, as before. Melee points are part of the saved profile, so the max is right on every load. Levelling up doesn't touch Energy.
- **HUD:** the existing Energy bar ("Energy: current / max", same style and place) now eases smoothly between the server's updates, at any frame rate.

**Existing worlds:** nothing needs migrating. Max Energy is recalculated from the saved Melee points. Players who relied on level-based Energy (100 + 2 per level) now have 100 + 5 per Melee point.

**Movement:** Ember Wings flight drains Energy per tick, now a fixed amount per tick. Dashes are fruit or weapon moves with their own costs. There's no Flash Step ability in the mod yet.
