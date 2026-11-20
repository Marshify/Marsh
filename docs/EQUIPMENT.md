# Equipment ownership, the Backpack and durability (0.35.0, Backpack equipment 0.57.0)

## No durability

`mixin/ItemStackNoDurabilityMixin` makes no item damageable, on server and client. Nothing wears out or breaks: attacking, abilities, fishing, using items on blocks or entities. There are no durability bars, no durability tooltip line and nothing to repair. Damaged items in old saves are mended to full the first time the player is checked (on join, then each second).

## Owned equipment

Every sword, gun and accessory in `Gear`, and the Starter Fishing Rod (owned equipment since 0.57.0), is an unlock owned
once, for good, under its registered id:
- **Swords:** `grandline:katana`, `grandline:cutlass`, `grandline:saber`, `grandline:trident`.
- **Guns:** `grandline:slingshot`, `grandline:flintlock`, `grandline:musket`, `grandline:cannon`.
- **Fishing rod:** `grandline:starter_fishing_rod`.
- **Accessories:** `grandline:black_cape`, `grandline:pink_coat`, `grandline:swordsman_hat`, `grandline:tomoe_ring`.

**Where it's stored.** Ownership is the `grandline:owned_equipment` player record. It's saved with the player and kept through death, respawn, logout and restart. The item in the inventory, the loadout (slot 3 for the sword, slot 4 for the gun) or the accessory slot is only how an owned item is held. Equipping and unequipping never change ownership.

**API** (`gear/EquipmentOwnership`):
- `owns(player, item | id)`, `owned(player)`
- `grant(player, item)`, which returns GRANTED (a weapon: equipped, as none of its kind was), GRANTED_NO_ROOM, STORED
  (a weapon of its kind is already equipped: it waits in the Backpack), ALREADY_OWNED or NOT_EQUIPMENT
- `setEquipped(player, item, equip)` (the Backpack's Equip / Unequip; returns EQUIPPED, UNEQUIPPED, ALREADY, NOT_OWNED,
  NOT_WEAPON, NOT_EQUIPPED or NO_ROOM), `equipped(player, category)`, `isEquipped(player, item)`
- `equip(player, item)` (equip and take in hand), `unequip(player, gearSlot)` (accessories)
- `revoke(player, item)` (admin only)

**One weapon of each kind equipped** (`WeaponCategory`: sword, gun, rod). The record also holds the equipped weapon of
each category. Only equipped weapons are items: swords and guns in hotbar slots 1-4 (the loadout keys), the rod in a
hotbar slot. Every other owned weapon is kept in the Backpack and is not an item anywhere, so it can't be duplicated,
dropped or lost. The fighting style and fruit already have one slot each; accessories keep their gear slots.

**Reconciler.** It runs each second per player, and on join and respawn. Whatever happened to the stacks (pickup,
containers, the crafting grid, the cursor, dragging, commands, death), it enforces:
- each equipped weapon and owned accessory is held exactly once (the name of the held copy is kept);
- weapons in the Backpack are never held: any copy is removed;
- extra copies are removed, keeping the equipped, then selected, then hotbar copy;
- a lost equipped weapon comes back in its kind's slots when there's room;
- equipment the player doesn't own is removed (in creative mode, taking it from the creative menu makes it owned, and a
  weapon equipped).

**Drops and pickups.** Equipment can't be dropped (it goes straight back into the inventory). A loose equipment item can't be picked up.

**Sources:**

| Source | Behaviour |
|---|---|
| Swordsmith and Gunsmith | Owned items are listed as "Owned" with no buy option. The server refuses them before taking any money ("already owned"). A new weapon is equipped if none of its kind is, otherwise it goes to the Backpack ("added to your Backpack"). |
| Fisherman | Sells the rod once; an owned rod is listed as "Owned" ("rod_owned"). |
| Saber Warden | The top fighter nearby who doesn't own a Saber gets it. Owners are told they already own it and keep the fight's other rewards. Nothing is dropped loose. |
| Quest, treasure and fishing rewards | None of these give equipment today. Any future reward should call `grant`. |
| Admin | `/grandline equipment list <player>`, `grant <player> <item>` (respects ownership), `revoke <player> <item>`. Vanilla `/give` of equipment the player doesn't own is removed within a second. |

**Migration.** The first time a player is checked, everything they hold or wear counts as owned, and duplicates are merged to one. It runs once per player (there's a flag) and running it again changes nothing. Nothing else in the inventory, and no money, levels, quests or mastery, is touched.

**Backpack migration (0.57.0, `equip_version`).** Once per player: in each category, the owned weapon held most
prominently (in hand, then the hotbar, then the inventory) becomes the equipped one; the others stay owned, in the
Backpack. A held Starter Fishing Rod becomes owned and equipped. Nothing is removed from ownership.

## The Backpack (Items → Backpack)

- Shows every owned weapon (equipped or not), then carried fruits and accessories, then the fish bag; scrolls past 28.
- Equipped weapons have a green check in the corner and a subtle green outline.
- Selecting an item opens its details: texture, name, kind, rarity, mastery (Fishing Mastery for the rod), status
  (Equipped / In Backpack, and which weapon it would replace) and an Equip or Unequip button. Accessories get Equip
  (they're taken off in Build).
- Equip / Unequip only ask the server (`EquipWeaponPayload`, rate limited, validated in `setEquipped`); the screen shows
  the server's answer (`EquipmentSyncPayload`). The button locks until the answer arrives and then until the mouse
  leaves it, so a burst of clicks does one thing.
- Equipping a weapon puts it in the hotbar slot of the one it replaces (the custom arrangement and the TAB loadout order
  are kept; if the old one was in hand, the new one is, and the ability panel switches). Unequipping takes it out of the
  hotbar. Mastery and cooldowns belong to the weapon's power and are never touched.

## Not changed

- The Build tab, the accessory slots and the TAB loadout order editor.