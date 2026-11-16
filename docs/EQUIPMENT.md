# Equipment ownership and durability (0.35.0)

## No durability

`mixin/ItemStackNoDurabilityMixin` makes no item damageable, on server and client. Nothing wears out or breaks: attacking, abilities, fishing, using items on blocks or entities. There are no durability bars, no durability tooltip line and nothing to repair. Damaged items in old saves are mended to full the first time the player is checked (on join, then each second).

## Owned equipment

Every sword, gun and accessory in `Gear` is an unlock owned once, for good, under its registered id:
- **Swords:** `grandline:katana`, `grandline:cutlass`, `grandline:saber`, `grandline:trident`.
- **Guns:** `grandline:slingshot`, `grandline:flintlock`, `grandline:musket`, `grandline:cannon`.
- **Accessories:** `grandline:black_cape`, `grandline:pink_coat`, `grandline:swordsman_hat`, `grandline:tomoe_ring`.

**Where it's stored.** Ownership is the `grandline:owned_equipment` player record. It's saved with the player and kept through death, respawn, logout and restart. The item in the inventory, the loadout (slot 3 for the sword, slot 4 for the gun) or the accessory slot is only how an owned item is held. Equipping and unequipping never change ownership.

**API** (`gear/EquipmentOwnership`):
- `owns(player, item | id)`, `owned(player)`
- `grant(player, item)`, which returns GRANTED, GRANTED_NO_ROOM, ALREADY_OWNED or NOT_EQUIPMENT
- `equip(player, item)`, `unequip(player, gearSlot)`
- `revoke(player, item)` (admin only)

**Reconciler.** It runs each second per player, and on join and respawn. Whatever happened to the stacks (pickup, containers, the crafting grid, dragging, commands), it enforces:
- each owned item is held exactly once;
- extra copies are removed, keeping the equipped, then selected, then hotbar copy;
- a lost owned item comes back when there's room;
- equipment the player doesn't own is removed.

**Drops and pickups.** Equipment can't be dropped (it goes straight back into the inventory). A loose equipment item can't be picked up.

**Sources:**

| Source | Behaviour |
|---|---|
| Swordsmith and Gunsmith | Owned items are listed as "Owned" with no buy option. The server refuses them before taking any money ("already owned"). |
| Saber Warden | The top fighter nearby who doesn't own a Saber gets it. Owners are told they already own it and keep the fight's other rewards. Nothing is dropped loose. |
| Quest, treasure and fishing rewards | None of these give equipment today. Any future reward should call `grant`. |
| Admin | `/grandline equipment list <player>`, `grant <player> <item>` (respects ownership), `revoke <player> <item>`. Vanilla `/give` of equipment the player doesn't own is removed within a second. |

**Migration.** The first time a player is checked, everything they hold or wear counts as owned, and duplicates are merged to one. It runs once per player (there's a flag) and running it again changes nothing. Nothing else in the inventory, and no money, levels, quests or mastery, is touched.

## Not changed

- The Items screen tabs (Backpack, Build). Owned items already appear there once, because exactly one copy is held.
- The accessory slots and the hotbar loadout behaviour.
